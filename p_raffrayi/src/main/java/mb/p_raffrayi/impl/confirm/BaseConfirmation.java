package mb.p_raffrayi.impl.confirm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.Futures;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Set;

import org.metaborg.util.future.AggregateFuture.SC;

import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.impl.envdiff.AddedEdge;
import mb.p_raffrayi.impl.envdiff.RemovedEdge;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.patching.IPatchCollection;
import mb.scopegraph.patching.PatchCollection;

abstract class BaseConfirmation<S, L, D> implements IConfirmation<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(BaseConfirmation.class);

    protected final IConfirmationContext<S, L, D> context;

    protected BaseConfirmation(IConfirmationContext<S, L, D> context) {
        this.context = context;
    }

    private final SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>> DENY = SC.shortCircuit(ConfirmResult.deny());
    private final SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>> ACC_NO_PATCHES = SC.of(ConfirmResult.confirm());

    @Override public IFuture<ConfirmResult<S, L, D>> confirm(IRecordedQuery<S, L, D> query) {
        logger.debug("Confirming {}.", query);
        CompletableFuture<ConfirmResult<S, L, D>> result = new CompletableFuture<>();
        confirm(query.source(), query.labelWf(), query.dataWf(), query.empty()).whenComplete((r, ex) -> {
            if(ex != null) {
                result.completeExceptionally(ex);
                return;
            }
            r.visit(() -> result.complete(r), (addedQueries, removedQueries, resultPatches, globalPatches) -> {
                Futures.<S, IPatchCollection.Immutable<S>>reduce(PatchCollection.Immutable.of(), query.datumScopes(),
                        (acc, scope) -> {
                            return context.match(scope).thenApply(newScopeOpt -> {
                                final S newScope = newScopeOpt.orElseThrow(() -> new IllegalStateException(
                                        "Cannot have a missing datum scope match when all edge are confirmed."));
                                return acc.put(newScope, scope);
                            });
                        }).whenComplete((datumPatches, ex2) -> {
                            if(ex2 != null) {
                                result.completeExceptionally(ex2);
                            } else if(query.includePatches()) {
                                result.complete(ConfirmResult.confirm(addedQueries, removedQueries,
                                        resultPatches.putAll(datumPatches), globalPatches));
                            } else {
                                result.complete(ConfirmResult.confirm(addedQueries, removedQueries, resultPatches,
                                        globalPatches.putAll(datumPatches)));
                            }
                        });
            });
        });

        return result;
    }

    @Override public IFuture<ConfirmResult<S, L, D>> confirm(S scope, LabelWf<L> labelWF, DataWf<S, L, D> dataWF,
            boolean prevEnvEmpty) {
        return context.externalConfirm(scope, labelWF, dataWF, prevEnvEmpty).thenCompose(conf -> {
            return conf.map(CompletableFuture::completedFuture)
                    .orElseGet(() -> localConfirm(scope, labelWF, dataWF, prevEnvEmpty));
        });
    }

    private IFuture<ConfirmResult<S, L, D>> localConfirm(S scope, LabelWf<L> labelWf, DataWf<S, L, D> dataWf,
            boolean prevEnvEmpty) {
        final ICompletableFuture<ConfirmResult<S, L, D>> result = new CompletableFuture<>();
        context.envDiff(scope, labelWf).whenComplete((envDiff, ex) -> {
            if(ex != null) {
                logger.error("Environment diff for {}/{} failed.", scope, labelWf, ex);
                result.completeExceptionally(ex);
            } else {
                logger.debug("Environment diff for {}/{} completed.", scope, labelWf, ex);
                logger.trace("value: {}.", envDiff);
                final ArrayList<IFuture<SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>>>> futures =
                        new ArrayList<>();

                // Include patches of path into patch set
                futures.add(CompletableFuture.completedFuture(
                        SC.of(ConfirmResult.confirm(PatchCollection.Immutable.of(envDiff.patches())))));
                final LazyFuture<Optional<DataWf<S, L, D>>> patchedDataWf = new LazyFuture<>(() -> patchDataWf(dataWf));

                // Verify each added/removed edge.
                envDiff.changes().forEach(diff -> {
                    // @formatter:off
                    futures.add(diff.<IFuture<SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>>>>match(
                        addedEdge -> handleAddedEdge(addedEdge, patchedDataWf),
                        removedEdge -> handleRemovedEdge(removedEdge, dataWf, prevEnvEmpty)
                    ));
                    // @formatter:on
                });

                AggregateFuture.ofShortCircuitable(this::merge, futures).whenComplete(result::complete);
            }
        });

        return result.whenComplete((res, ex) -> {
            logger.debug("Environment diff completed: {}.", res);
        });
    }

    protected abstract IFuture<SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>>>
            handleAddedEdge(AddedEdge<S, L, D> addedEdge, LazyFuture<Optional<DataWf<S, L, D>>> dataWf);

    protected abstract IFuture<SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>>>
            handleRemovedEdge(RemovedEdge<S, L, D> removedEdge, DataWf<S, L, D> dataWf, boolean prevEnvEnpty);

    protected SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>> deny() {
        return DENY;
    }

    protected SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>> acceptAdded(
            java.util.Set<IRecordedQuery<S, L, D>> transitiveQueries,
            java.util.Set<IRecordedQuery<S, L, D>> predicateQueries) {
        if(transitiveQueries.isEmpty() && predicateQueries.isEmpty()) {
            return ACC_NO_PATCHES;
        }
        final Set.Transient<IRecordedQuery<S, L, D>> addedQueries = CapsuleUtil.transientSet();
        addedQueries.__insertAll(transitiveQueries);
        addedQueries.__insertAll(predicateQueries);
        return SC.of(ConfirmResult.confirm(addedQueries.freeze(), CapsuleUtil.immutableSet(),
                PatchCollection.Immutable.of()));
    }

    protected SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>> acceptRemoved(
            java.util.Set<IRecordedQuery<S, L, D>> transitiveQueries,
            java.util.Set<IRecordedQuery<S, L, D>> predicateQueries) {
        if(transitiveQueries.isEmpty() && predicateQueries.isEmpty()) {
            return ACC_NO_PATCHES;
        }
        final Set.Transient<IRecordedQuery<S, L, D>> removedQueries = CapsuleUtil.transientSet();
        removedQueries.__insertAll(transitiveQueries);
        removedQueries.__insertAll(predicateQueries);
        return SC.of(ConfirmResult.confirm(CapsuleUtil.immutableSet(), removedQueries.freeze(),
                PatchCollection.Immutable.of()));
    }

    protected IFuture<SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>>> denyFuture() {
        return CompletableFuture.completedFuture(DENY);
    }

    protected IFuture<SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>>> acceptFuture() {
        return CompletableFuture.completedFuture(ACC_NO_PATCHES);
    }

    protected IFuture<SC<IPatchCollection.Immutable<S>, ConfirmResult<S, L, D>>>
            accept(IPatchCollection.Immutable<S> patches) {
        return CompletableFuture.completedFuture(SC.of(patches));
    }

    protected ConfirmResult<S, L, D> merge(List<ConfirmResult<S, L, D>> confirmResults) {
        // Patch sets should be build from matches by scope differ, so just adding them is safe.
        return confirmResults.stream().reduce(ConfirmResult.confirm(), ConfirmResult::add);
    }

    private SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>> toSC(ConfirmResult<S, L, D> intermediate) {
        return intermediate.match(() -> SC.shortCircuit(intermediate),
                (addQ, remQ, resP, globP) -> SC.of(intermediate));
    }

    protected IFuture<SC<ConfirmResult<S, L, D>, ConfirmResult<S, L, D>>>
            toSCFuture(IFuture<ConfirmResult<S, L, D>> intermediateFuture) {
        return intermediateFuture.thenApply(this::toSC);
    }

    private IFuture<Optional<DataWf<S, L, D>>> patchDataWf(DataWf<S, L, D> dataWf) {
        final Set.Immutable<S> scopes = dataWf.scopes();
        if(scopes.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.of(dataWf));
        }

        final List<IFuture<SC<Tuple2<S, S>, Optional<DataWf<S, L, D>>>>> futures = new ArrayList<>();
        for(S scope : scopes) {
            // @formatter:off
            futures.add(context.match(scope)
                .<SC<Tuple2<S, S>, Optional<DataWf<S, L, D>>>>thenApply(match -> match
                    .map(m -> SC.<Tuple2<S, S>, Optional<DataWf<S, L, D>>>of(Tuple2.of(scope, m)))
                    .orElse(SC.shortCircuit(Optional.empty()))
                ));
            // @formatter:on
        }

        return AggregateFuture.<Tuple2<S, S>, Optional<DataWf<S, L, D>>>ofShortCircuitable(patches -> {
            return Optional.of(dataWf.patch(PatchCollection.Immutable.<S>of().putAll(patches)));
        }, futures);
    }

    class LazyFuture<T> {

        private Supplier<IFuture<T>> supplier;

        private IFuture<T> value;

        public LazyFuture(Supplier<IFuture<T>> supplier) {
            this.supplier = supplier;
        }

        public IFuture<T> get() {
            if(value == null) {
                value = supplier.get();
                supplier = null;
            }
            return value;
        }

    }

}
