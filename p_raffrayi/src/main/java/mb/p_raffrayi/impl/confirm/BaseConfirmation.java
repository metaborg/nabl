package mb.p_raffrayi.impl.confirm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.future.AggregateFuture.SC;

import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.impl.envdiff.AddedEdge;
import mb.p_raffrayi.impl.envdiff.External;
import mb.p_raffrayi.impl.envdiff.RemovedEdge;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

abstract class BaseConfirmation<S, L, D> implements IConfirmation<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(BaseConfirmation.class);

    protected final IConfirmationContext<S, L, D> context;

    protected BaseConfirmation(IConfirmationContext<S, L, D> context) {
        this.context = context;
    }

    private final SC<? extends BiMap.Immutable<S>, ? extends ConfirmResult<S>> DENY =
            SC.shortCircuit(ConfirmResult.deny());
    private final SC<? extends BiMap.Immutable<S>, ? extends ConfirmResult<S>> ACC_NO_PATCHES =
            SC.of(BiMap.Immutable.of());

    @Override public IFuture<ConfirmResult<S>> confirm(java.util.Set<IRecordedQuery<S, L, D>> queries) {
        final List<IFuture<SC<? extends BiMap.Immutable<S>, ? extends ConfirmResult<S>>>> futures =
                queries.stream().map(this::confirm).map(this::toSCFuture).collect(Collectors.toList());

        return AggregateFuture.ofShortCircuitable(this::merge, futures);
    }

    public abstract IFuture<ConfirmResult<S>> confirm(IRecordedQuery<S, L, D> query);

    protected IFuture<ConfirmResult<S>> confirmSingle(IRecordedQuery<S, L, D> query) {
        logger.debug("Confirming {}.", query);
        return confirm(query.scopePath(), query.labelWf(), query.dataWf(), query.result().map(Env::isEmpty).orElse(false));
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
        context.envDiff(path, labelWf, dataWf).whenComplete((envDiff, ex) -> {
            if(ex != null) {
                logger.error("Environment diff for {}/{} failed.", path, labelWf, ex);
                result.completeExceptionally(ex);
            } else {
                logger.debug("Environment diff for {}/{} completed.", path, labelWf, ex);
                logger.trace("value: {}.", envDiff);
                final ArrayList<IFuture<SC<? extends BiMap.Immutable<S>, ? extends ConfirmResult<S>>>> futures =
                        new ArrayList<>();
                futures.add(CompletableFuture.completedFuture(SC.of(envDiff.patches())));
                envDiff.diffPaths().forEach(diffPath -> {
                    // @formatter:off
                    futures.add(diffPath.getDatum().<IFuture<SC<? extends BiMap.Immutable<S>, ? extends ConfirmResult<S>>>>match(
                        addedEdge -> handleAddedEdge(addedEdge),
                        removedEdge -> handleRemovedEdge(removedEdge, prevEnvEmpty),
                        external -> handleExternal(external),
                        diffTree -> {
                            logger.error("Diff path cannot end in subtree: {}.", diffPath);
                            throw new IllegalStateException("Diff path cannot end in subtree");
                        }
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

    protected abstract IFuture<SC<? extends BiMap.Immutable<S>, ? extends ConfirmResult<S>>>
            handleAddedEdge(AddedEdge<S, L, D> addedEdge);

    protected abstract IFuture<SC<? extends BiMap.Immutable<S>, ? extends ConfirmResult<S>>>
            handleRemovedEdge(RemovedEdge<S, L, D> removedEdge, boolean prevEnvEnpty);

    protected abstract IFuture<SC<? extends BiMap.Immutable<S>, ? extends ConfirmResult<S>>>
            handleExternal(External<S, L, D> external);

    protected SC<? extends BiMap.Immutable<S>, ? extends ConfirmResult<S>> deny() {
        return DENY;
    }

    protected SC<? extends BiMap.Immutable<S>, ? extends ConfirmResult<S>> accept() {
        return ACC_NO_PATCHES;
    }

    protected IFuture<SC<? extends BiMap.Immutable<S>, ? extends ConfirmResult<S>>> acceptFuture() {
        return CompletableFuture.completedFuture(ACC_NO_PATCHES);
    }

    protected IFuture<SC<? extends BiMap.Immutable<S>, ? extends ConfirmResult<S>>>
            accept(BiMap.Immutable<S> patches) {
        return CompletableFuture.completedFuture(SC.of(patches));
    }

    private ConfirmResult<S> merge(List<BiMap.Immutable<S>> patchSets) {
        // Patch sets should be build from matches by scope differ, so just adding them is safe.
        return ConfirmResult.confirm(patchSets.stream().reduce(BiMap.Immutable.of(), BiMap.Immutable::putAll));
    }

    private SC<BiMap.Immutable<S>, ConfirmResult<S>> toSC(ConfirmResult<S> intermediate) {
        return intermediate.match(() -> SC.shortCircuit(ConfirmResult.deny()), SC::of);
    }

    private IFuture<SC<? extends BiMap.Immutable<S>, ? extends ConfirmResult<S>>>
            toSCFuture(IFuture<ConfirmResult<S>> intermediateFuture) {
        return intermediateFuture.thenApply(this::toSC);
    }

}