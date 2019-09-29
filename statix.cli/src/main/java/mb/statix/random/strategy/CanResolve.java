package mb.statix.random.strategy;

import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.util.functions.Predicate2;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CResolveQuery;
import mb.statix.random.FocusedSearchState;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.random.scopegraph.DataWF;
import mb.statix.random.scopegraph.NameResolution;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.query.RegExpLabelWF;
import mb.statix.solver.query.RelationLabelOrder;

final class CanResolve extends SearchStrategy<FocusedSearchState<CResolveQuery>, FocusedSearchState<CResolveQuery>> {

    @Override protected SearchNodes<FocusedSearchState<CResolveQuery>> doApply(SearchContext ctx,
            FocusedSearchState<CResolveQuery> input, SearchNode<?> parent) {
        final IState.Immutable state = input.state();
        final IUnifier unifier = state.unifier();
        final CResolveQuery query = input.focus();

        final Scope scope = Scope.matcher().match(query.scopeTerm(), unifier).orElse(null);
        if(scope == null) {
            return SearchNodes.empty(parent, this.toString() + "[no scope]");
        }

        final Boolean isAlways;
        try {
            isAlways = query.min().getDataEquiv().isAlways(state.spec()).orElse(null);
        } catch(InterruptedException e) {
            throw new MetaborgRuntimeException(e);
        }
        if(isAlways == null) {
            return SearchNodes.empty(parent, this.toString() + "[cannot decide data equivalence]");
        }

        final ICompleteness.Immutable completeness = input.completeness();
        final Predicate2<Scope, ITerm> isComplete2 = (s, l) -> completeness.isComplete(s, l, state.unifier());
        final LabelWF<ITerm> labelWF = RegExpLabelWF.of(query.filter().getLabelWF());
        final LabelOrder<ITerm> labelOrd = new RelationLabelOrder(query.min().getLabelOrder());
        final DataWF<ITerm, CEqual> dataWF = new ResolveDataWF(state, completeness, query.filter().getDataWF(), query);

        // @formatter:off
        final NameResolution<Scope, ITerm, ITerm, CEqual> nameResolution = new NameResolution<>(
                state.scopeGraph(), query.relation(),
                labelWF, labelOrd, isComplete2,
                dataWF, isAlways, isComplete2);
        // @formatter:on

        try {
            nameResolution.resolve(scope, () -> false);
        } catch(ResolutionException e) {
            return SearchNodes.empty(parent, this.toString() + "[cannot resolve]");
        } catch(InterruptedException e) {
            throw new MetaborgRuntimeException(e);
        }

        return SearchNodes.of(parent, new SearchNode<>(ctx.nextNodeId(), input, parent, parent.desc()));
    }

    @Override public String toString() {
        return "can-resolve";
    }

}