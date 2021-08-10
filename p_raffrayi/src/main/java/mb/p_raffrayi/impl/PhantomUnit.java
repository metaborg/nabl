package mb.p_raffrayi.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.diff.IScopeGraphDiffer;
import mb.p_raffrayi.impl.diff.RemovingDiffer;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.BiMap.Immutable;

public class PhantomUnit<S, L, D> extends AbstractUnit<S, L, D, Unit> {

    private final IUnitResult<S, L, D, ?> previousResult;

    public PhantomUnit(IActor<? extends IUnit<S, L, D, Unit>> self, IActorRef<? extends IUnit<S, L, D, ?>> parent,
            IUnitContext<S, L, D> context, Iterable<L> edgeLabels, IUnitResult<S, L, D, ?> previousResult) {
        super(self, parent, context, edgeLabels);
        this.previousResult = previousResult;
    }

    @Override public IFuture<IUnitResult<S, L, D, Unit>> _start(List<S> rootScopes) {
        doStart(rootScopes, previousResult.rootScopes());

        // Add Phantom unit for all previous subunits.
        for(Map.Entry<String, IUnitResult<S, L, D, ?>> entry : previousResult.subUnitResults().entrySet()) {
            this.<Unit>doAddSubUnit(entry.getKey(),
                    (subself, subcontext) -> new PhantomUnit<>(subself, self, subcontext, edgeLabels, entry.getValue()),
                    new ArrayList<>(), true);
        }

        return doFinish(CompletableFuture.completedFuture(Unit.unit));
    }

    @Override public IFuture<StateSummary<S>> _requireRestart() {
        return CompletableFuture.completedFuture(StateSummary.released());
    }

    @Override public void _release(Immutable<S> patches) {
        // ignore
    }

    @Override public void _restart() {
        // ignore
    }

    @Override public IFuture<Optional<Immutable<S>>> _confirm(S scope, Set.Immutable<S> seenScopes, LabelWf<L> labelWF,
            DataWf<S, L, D> dataWF, boolean prevEnvEmpty) {
        // TODO: execute query in old scope graph, and return {} when result is empty?
        return CompletableFuture.completedFuture(prevEnvEmpty ? Optional.of(BiMap.Immutable.of()): Optional.empty());
    }

    @Override protected IFuture<D> getExternalDatum(D datum) {
        return CompletableFuture.completedFuture(datum);
    }

    @Override protected IScopeGraphDiffer<S, L, D> initDiffer() {
        return new RemovingDiffer<>(previousResult.scopeGraph(), differOps());
    }

}
