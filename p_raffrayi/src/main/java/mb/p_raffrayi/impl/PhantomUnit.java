package mb.p_raffrayi.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.ITypeChecker.IOutput;
import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.confirm.ConfirmResult;
import mb.p_raffrayi.impl.diff.RemovingDiffer;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.p_raffrayi.nameresolution.IQuery;
import mb.p_raffrayi.nameresolution.NameResolutionQuery;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;
import mb.scopegraph.patching.PatchCollection;

public class PhantomUnit<S, L, D> extends AbstractUnit<S, L, D, Unit> {

    private final IUnitResult<S, L, D, ? extends IOutput<S, L, D>> previousResult;

    public PhantomUnit(IActor<? extends IUnit<S, L, D, Unit>> self, IActorRef<? extends IUnit<S, L, D, ?>> parent,
            IUnitContext<S, L, D> context, Iterable<L> edgeLabels,
            IUnitResult<S, L, D, ? extends IOutput<S, L, D>> previousResult) {
        super(self, parent, context, edgeLabels);
        this.previousResult = previousResult;
    }

    @SuppressWarnings("unchecked") @Override public IFuture<IUnitResult<S, L, D, Unit>> _start(List<S> rootScopes) {
        doStart(rootScopes);
        initDiffer(new RemovingDiffer<>(previousResult.scopeGraph(), differOps()), rootScopes,
                previousResult.rootScopes());

        // Add Phantom unit for all previous subunits.
        for(Map.Entry<String, IUnitResult<S, L, D, ?>> entry : previousResult.subUnitResults().entrySet()) {
            this.<Unit>doAddSubUnit(entry.getKey(),
                    (subself, subcontext) -> new PhantomUnit<>(subself, self, subcontext, edgeLabels,
                            (IUnitResult<S, L, D, ? extends IOutput<S, L, D>>) entry.getValue()),
                    new ArrayList<>(), true);
        }

        return doFinish(CompletableFuture.completedFuture(Unit.unit));
    }

    @Override public IFuture<IQueryAnswer<S, L, D>> _queryPrevious(ScopePath<S, L> path, IQuery<S, L, D> query,
            DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv) {
        return doQueryPrevious(self.sender(TYPE), previousResult.scopeGraph(), path, query, dataWF, dataEquiv);
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

    @Override public IFuture<ConfirmResult<S, L, D>> _confirm(ScopePath<S, L> path, LabelWf<L> labelWF,
            DataWf<S, L, D> dataWF, boolean prevEnvEmpty) {
        if(prevEnvEmpty) {
            return CompletableFuture.completedFuture(ConfirmResult.confirm());
        }
        return doQueryPrevious(self.sender(TYPE), previousResult.scopeGraph(), path,
                new NameResolutionQuery<>(labelWF, LabelOrder.none(), edgeLabels), dataWF, DataLeq.any())
                        .thenApply(ans -> {
                            if(!ans.env().isEmpty()) {
                                return ConfirmResult.deny();
                            }
                            final Set.Transient<IRecordedQuery<S, L, D>> queries = CapsuleUtil.transientSet();
                            queries.__insertAll(ans.transitiveQueries());
                            queries.__insertAll(ans.predicateQueries());
                            return ConfirmResult.confirm(CapsuleUtil.immutableSet(), queries.freeze(),
                                    PatchCollection.Immutable.of());
                        });
    }

    @Override protected IFuture<D> getExternalDatum(D datum) {
        return CompletableFuture.completedFuture(datum);
    }

    @Override protected D getPreviousDatum(D datum) {
        return previousResult.result().getExternalRepresentation(datum);
    }

}
