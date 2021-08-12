package mb.p_raffrayi.impl.confirm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.future.AggregateFuture.SC;

import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.impl.envdiff.AddedEdge;
import mb.p_raffrayi.impl.envdiff.External;
import mb.p_raffrayi.impl.envdiff.RemovedEdge;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

abstract class BaseConfirmation<S, L, D> implements IConfirmation<S, L, D> {

    protected final IConfirmationContext<S, L, D> context;

    protected BaseConfirmation(IConfirmationContext<S, L, D> context) {
        this.context = context;
    }

    private final SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>> DENY =
            SC.shortCircuit(Optional.empty());
    private final SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>> ACC_NO_PATCHES =
            SC.of(BiMap.Immutable.of());

    @Override public IFuture<Optional<BiMap.Immutable<S>>> confirm(java.util.Set<IRecordedQuery<S, L, D>> queries) {
        final List<IFuture<SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>>>> futures =
                queries.stream().map(this::confirm).map(this::toSCFuture).collect(Collectors.toList());

        return AggregateFuture.ofShortCircuitable(this::merge, futures);
    }

    public abstract IFuture<Optional<BiMap.Immutable<S>>> confirm(IRecordedQuery<S, L, D> query);

    protected IFuture<Optional<BiMap.Immutable<S>>> confirmSingle(IRecordedQuery<S, L, D> query) {
        return confirm(query.scopePath(), query.labelWf(), query.dataWf(), query.result().isEmpty());
    }

    @Override public IFuture<Optional<BiMap.Immutable<S>>> confirm(ScopePath<S, L> path, LabelWf<L> labelWF,
            DataWf<S, L, D> dataWF, boolean prevEnvEmpty) {
        return context.externalConfirm(path, labelWF, dataWF, prevEnvEmpty).thenCompose(conf -> {
            // @formatter:off
            return conf.map(c -> c.match(
                    () -> CompletableFuture.completedFuture(Optional.<BiMap.Immutable<S>>empty()),
                    patches -> CompletableFuture.completedFuture(Optional.of(patches))
                ))
                .orElseGet(() -> localConfirm(path, labelWF, dataWF, prevEnvEmpty));
            // @formatter:on
        });
    }

    private IFuture<Optional<BiMap.Immutable<S>>> localConfirm(ScopePath<S, L> path, LabelWf<L> labelWF,
            DataWf<S, L, D> dataWF, boolean prevEnvEmpty) {
        final ICompletableFuture<Optional<BiMap.Immutable<S>>> result = new CompletableFuture<>();
        context.envDiff(path, labelWF, dataWF).whenComplete((envDiff, ex2) -> {
            if(ex2 != null) {
                result.completeExceptionally(ex2);
            } else {
                final ArrayList<IFuture<SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>>>> futures =
                        new ArrayList<>();
                envDiff.diffPaths().forEach(diffPath -> {
                    // @formatter:off
                    futures.add(diffPath.getDatum().<IFuture<SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>>>>match(
                        addedEdge -> handleAddedEdge(addedEdge),
                        removedEdge -> handleRemovedEdge(removedEdge, prevEnvEmpty),
                        external -> handleExternal(external),
                        diffTree -> { throw new IllegalStateException("Diff path cannot end in subtree"); }
                    ));
                    // @formatter:on
                });

                AggregateFuture.<BiMap.Immutable<S>, Optional<BiMap.Immutable<S>>>ofShortCircuitable(this::merge,
                        futures).whenComplete(result::complete);
            }
        });

        return result;
    }

    protected abstract IFuture<SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>>>
            handleAddedEdge(AddedEdge<S, L, D> addedEdge);

    protected abstract IFuture<SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>>>
            handleRemovedEdge(RemovedEdge<S, L, D> removedEdge, boolean prevEnvEnpty);

    protected abstract IFuture<SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>>>
            handleExternal(External<S, L, D> external);

    protected SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>> deny() {
        return DENY;
    }

    protected SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>> accept() {
        return ACC_NO_PATCHES;
    }

    protected IFuture<SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>>> acceptFuture() {
        return CompletableFuture.completedFuture(ACC_NO_PATCHES);
    }

    protected IFuture<SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>>>
            accept(BiMap.Immutable<S> patches) {
        return CompletableFuture.completedFuture(SC.of(patches));
    }

    private Optional<BiMap.Immutable<S>> merge(List<BiMap.Immutable<S>> patchSets) {
        // Patch sets should be build from matches by scope differ, so just adding them is safe.
        return Optional.of(patchSets.stream().reduce(BiMap.Immutable.of(), BiMap.Immutable::putAll));
    }

    private SC<BiMap.Immutable<S>, Optional<BiMap.Immutable<S>>> toSC(Optional<BiMap.Immutable<S>> intermediate) {
        return intermediate.<SC<BiMap.Immutable<S>, Optional<BiMap.Immutable<S>>>>map(SC::of)
                .orElse(SC.shortCircuit(Optional.empty()));
    }

    private IFuture<SC<? extends BiMap.Immutable<S>, ? extends Optional<BiMap.Immutable<S>>>>
            toSCFuture(IFuture<Optional<BiMap.Immutable<S>>> intermediateFuture) {
        return intermediateFuture.thenApply(this::toSC);
    }

}