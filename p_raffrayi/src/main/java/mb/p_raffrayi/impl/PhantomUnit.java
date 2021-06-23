package mb.p_raffrayi.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.unit.Unit;

import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.diff.IDifferScopeOps;
import mb.p_raffrayi.impl.diff.IScopeGraphDiffer;
import mb.p_raffrayi.impl.diff.RemovingDiffer;
import mb.scopegraph.oopsla20.diff.BiMap.Immutable;

public class PhantomUnit<S, L, D> extends AbstractUnit<S, L, D, Unit> {

    private final IUnitResult<S, L, D, ?> previousResult;
    private final IDifferScopeOps<S, D> scopeOps;

    public PhantomUnit(IActor<? extends IUnit<S, L, D, Unit>> self, IActorRef<? extends IUnit<S, L, D, ?>> parent,
            IUnitContext<S, L, D> context, Iterable<L> edgeLabels, IUnitResult<S, L, D, ?> previousResult,
            IDifferScopeOps<S, D> scopeOps) {
        super(self, parent, context, edgeLabels);
        this.previousResult = previousResult;
        this.scopeOps = scopeOps;
    }

    @Override public IFuture<IUnitResult<S, L, D, Unit>> _start(List<S> rootScopes) {
        doStart(rootScopes, previousResult.rootScopes());

        // Add Phantom unit for all previous subunits.
        for(Map.Entry<String, IUnitResult<S, L, D, ?>> entry : previousResult.subUnitResults().entrySet()) {
            this.<Unit>doAddSubUnit(entry.getKey(), (subself, subcontext) -> new PhantomUnit<>(subself, self, subcontext,
                    edgeLabels, entry.getValue(), scopeOps), new ArrayList<>(), true);
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

    @Override protected IFuture<D> getExternalDatum(D datum) {
        return CompletableFuture.completedFuture(datum);
    }

    @Override protected IScopeGraphDiffer<S, L, D> initDiffer() {
        return new RemovingDiffer<>(previousResult.scopeGraph(), new DifferOps(scopeOps));
    }

}
