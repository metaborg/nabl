package mb.p_raffrayi.impl.confirm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;
import mb.scopegraph.patching.IPatchCollection;
import mb.scopegraph.patching.PatchCollection;

abstract class BaseConfirmation<S, L, D> implements IConfirmation<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(BaseConfirmation.class);

    protected final IConfirmationContext<S, L, D> context;

    protected BaseConfirmation(IConfirmationContext<S, L, D> context) {
        this.context = context;
    }

    private final SC<IPatchCollection.Immutable<S>, ConfirmResult<S>> DENY = SC.shortCircuit(ConfirmResult.deny());
    private final SC<IPatchCollection.Immutable<S>, ConfirmResult<S>> ACC_NO_PATCHES =
            SC.of(PatchCollection.Immutable.of());

    @Override public IFuture<ConfirmResult<S>> confirm(IRecordedQuery<S, L, D> query) {
        logger.debug("Confirming {}.", query);
        CompletableFuture<ConfirmResult<S>> result = new CompletableFuture<>();
        confirm(query.scopePath(), query.labelWf(), query.dataWf(), query.empty()).whenComplete((r, ex) -> {
            if(ex != null) {
                result.completeExceptionally(ex);
            }
            r.visit(() -> result.complete(r), patches -> {
                if(query.includePatches()) {
                    Futures.<S, IPatchCollection.Immutable<S>>reduce(patches, query.datumScopes(), (acc, scope) -> {
                        return context.match(scope).thenApply(newScopeOpt -> {
                            final S newScope = newScopeOpt.orElseThrow(() -> new IllegalStateException(
                                    "Cannot have a missing datum scope match when all edge are confirmed."));
                            return acc.put(newScope, scope);
                        });
                    }).whenComplete((accPatches, ex2) -> {
                        if(ex2 != null) {
                            result.completeExceptionally(ex2);
                        }
                        result.complete(ConfirmResult.confirm(accPatches));
                    });
                } else {
                    result.complete(ConfirmResult.confirm());
                }
            });
        });

        return result;
    }

    @Override public IFuture<ConfirmResult<S>> confirm(ScopePath<S, L> path, LabelWf<L> labelWF, DataWf<S, L, D> dataWF,
            boolean prevEnvEmpty) {
        return context.externalConfirm(path, labelWF, dataWF, prevEnvEmpty).thenCompose(conf -> {
            return conf.map(CompletableFuture::completedFuture)
                    .orElseGet(() -> localConfirm(path, labelWF, dataWF, prevEnvEmpty));
        });
    }

    private IFuture<ConfirmResult<S>> localConfirm(ScopePath<S, L> path, LabelWf<L> labelWf, DataWf<S, L, D> dataWf,
            boolean prevEnvEmpty) {
        final ICompletableFuture<ConfirmResult<S>> result = new CompletableFuture<>();
        context.envDiff(path, labelWf).whenComplete((envDiff, ex) -> {
            if(ex != null) {
                logger.error("Environment diff for {}/{} failed.", path, labelWf, ex);
                result.completeExceptionally(ex);
            } else {
                logger.debug("Environment diff for {}/{} completed.", path, labelWf, ex);
                logger.trace("value: {}.", envDiff);
                final ArrayList<IFuture<SC<IPatchCollection.Immutable<S>, ConfirmResult<S>>>> futures =
                        new ArrayList<>();

                // Include patches of path into patch set
                futures.add(CompletableFuture.completedFuture(SC.of(PatchCollection.Immutable.of(envDiff.patches()))));
                final LazyFuture<Optional<DataWf<S, L, D>>> patchedDataWf = new LazyFuture<>(() -> patchDataWf(dataWf));

                // Verify each added/removed edge.
                envDiff.changes().forEach(diff -> {
                    // @formatter:off
                    futures.add(diff.<IFuture<SC<IPatchCollection.Immutable<S>, ConfirmResult<S>>>>match(
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

    protected abstract IFuture<SC<IPatchCollection.Immutable<S>, ConfirmResult<S>>>
            handleAddedEdge(AddedEdge<S, L, D> addedEdge, LazyFuture<Optional<DataWf<S, L, D>>> dataWf);

    protected abstract IFuture<SC<IPatchCollection.Immutable<S>, ConfirmResult<S>>>
            handleRemovedEdge(RemovedEdge<S, L, D> removedEdge, DataWf<S, L, D> dataWf, boolean prevEnvEnpty);

    protected SC<IPatchCollection.Immutable<S>, ConfirmResult<S>> deny() {
        return DENY;
    }

    protected SC<IPatchCollection.Immutable<S>, ConfirmResult<S>> accept() {
        return ACC_NO_PATCHES;
    }

    protected IFuture<SC<IPatchCollection.Immutable<S>, ConfirmResult<S>>> denyFuture() {
        return CompletableFuture.completedFuture(DENY);
    }

    protected IFuture<SC<IPatchCollection.Immutable<S>, ConfirmResult<S>>> acceptFuture() {
        return CompletableFuture.completedFuture(ACC_NO_PATCHES);
    }

    protected IFuture<SC<IPatchCollection.Immutable<S>, ConfirmResult<S>>>
            accept(IPatchCollection.Immutable<S> patches) {
        return CompletableFuture.completedFuture(SC.of(patches));
    }

    protected ConfirmResult<S> merge(List<IPatchCollection.Immutable<S>> patchSets) {
        // Patch sets should be build from matches by scope differ, so just adding them is safe.
        return ConfirmResult
                .confirm(patchSets.stream().reduce(PatchCollection.Immutable.of(), IPatchCollection.Immutable::putAll));
    }

    private SC<IPatchCollection.Immutable<S>, ConfirmResult<S>> toSC(ConfirmResult<S> intermediate) {
        return intermediate.match(() -> SC.shortCircuit(ConfirmResult.deny()), SC::of);
    }

    protected IFuture<SC<IPatchCollection.Immutable<S>, ConfirmResult<S>>>
            toSCFuture(IFuture<ConfirmResult<S>> intermediateFuture) {
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
