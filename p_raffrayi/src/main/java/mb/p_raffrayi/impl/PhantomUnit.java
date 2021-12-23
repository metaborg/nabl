package mb.p_raffrayi.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.unit.Unit;

import mb.p_raffrayi.IResult;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.confirm.ConfirmResult;
import mb.p_raffrayi.impl.diff.RemovingDiffer;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public class PhantomUnit<S, L, D> extends AbstractUnit<S, L, D, IResult.Empty<S, L, D>, Unit> {

    private final IUnitResult<S, L, D, ?, ?> previousResult;

    public PhantomUnit(IActor<? extends IUnit<S, L, D, IResult.Empty<S, L, D>, Unit>> self, IActorRef<? extends IUnit<S, L, D, ?, ?>> parent,
            IUnitContext<S, L, D> context, Iterable<L> edgeLabels, IUnitResult<S, L, D, ?, ?> previousResult) {
        super(self, parent, context, edgeLabels);
        this.previousResult = previousResult;
    }

    @Override public IFuture<IUnitResult<S, L, D, IResult.Empty<S, L, D>, Unit>> _start(List<S> rootScopes) {
        doStart(rootScopes);
        initDiffer(new RemovingDiffer<>(previousResult.scopeGraph(), differOps()), rootScopes,
                previousResult.rootScopes());

        // Add Phantom unit for all previous subunits.
        for(Map.Entry<String, IUnitResult<S, L, D, ?, ?>> entry : previousResult.subUnitResults().entrySet()) {
            this.<IResult.Empty<S, L, D>, Unit>doAddSubUnit(entry.getKey(),
                    (subself, subcontext) -> new PhantomUnit<>(subself, self, subcontext, edgeLabels, entry.getValue()),
                    new ArrayList<>(), true);
        }

        return doFinish(CompletableFuture.completedFuture(IResult.Empty.of()));
    }

    @Override public IFuture<Env<S, L, D>> _queryPrevious(ScopePath<S, L> path, LabelWf<L> labelWF,
            DataWf<S, L, D> dataWF, LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv) {
        return doQueryPrevious(self.sender(TYPE), previousResult.scopeGraph(), path, labelWF, dataWF, labelOrder,
                dataEquiv);
    }

    @Override public IFuture<StateSummary<S, L, D>> _state() {
        return CompletableFuture.completedFuture(StateSummary.released(process, dependentSet()));
    }

    @Override public void _release() {
        // ignore
    }

    @Override public void _restart() {
        // ignore
    }

    @Override public IFuture<ConfirmResult<S>> _confirm(ScopePath<S, L> path, LabelWf<L> labelWF,
            DataWf<S, L, D> dataWF, boolean prevEnvEmpty) {
        if(prevEnvEmpty) {
            return CompletableFuture.completedFuture(ConfirmResult.confirm());
        }
        return doQueryPrevious(self.sender(TYPE), previousResult.scopeGraph(), path, labelWF, dataWF, LabelOrder.none(),
                DataLeq.any()).thenApply(env -> env.isEmpty() ? ConfirmResult.confirm() : ConfirmResult.deny());
    }

    @Override protected IFuture<D> getExternalDatum(D datum) {
        return CompletableFuture.completedFuture(datum);
    }

    @Override protected D getPreviousDatum(D datum) {
        return previousResult.analysis().getExternalRepresentation(datum);
    }

}
