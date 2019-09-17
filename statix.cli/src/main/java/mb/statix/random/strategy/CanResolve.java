package mb.statix.random.strategy;

import java.util.stream.Stream;

import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.util.functions.Predicate2;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CResolveQuery;
import mb.statix.random.FocusedSearchState;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchStrategy;
import mb.statix.random.scopegraph.DataWF;
import mb.statix.random.scopegraph.NameResolution;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.completeness.IncrementalCompleteness;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.query.RegExpLabelWF;
import mb.statix.solver.query.RelationLabelOrder;

final class CanResolve extends SearchStrategy<FocusedSearchState<CResolveQuery>, FocusedSearchState<CResolveQuery>> {

    @Override protected Stream<SearchNode<FocusedSearchState<CResolveQuery>>> doApply(SearchContext ctx,
            FocusedSearchState<CResolveQuery> input, SearchNode<?> parent) {
        final IState.Immutable state = input.state();
        final IUnifier unifier = state.unifier();
        final CResolveQuery query = input.focus();

        final Scope scope = Scope.matcher().match(query.scopeTerm(), unifier).orElse(null);
        if(scope == null) {
            return Stream.empty();
        }

        final Boolean isAlways;
        try {
            isAlways = query.min().getDataEquiv().isAlways(state.spec()).orElse(null);
        } catch(InterruptedException e) {
            throw new MetaborgRuntimeException(e);
        }
        if(isAlways == null) {
            return Stream.empty();
        }

        final ICompleteness completeness = new IncrementalCompleteness(state.spec());
        completeness.addAll(input.constraints(), unifier);
        final IsComplete isComplete3 = (s, l, st) -> completeness.isComplete(s, l, st.unifier());
        final Predicate2<Scope, ITerm> isComplete2 = (s, l) -> completeness.isComplete(s, l, state.unifier());
        final LabelWF<ITerm> labelWF = RegExpLabelWF.of(query.filter().getLabelWF());
        final LabelOrder<ITerm> labelOrd = new RelationLabelOrder(query.min().getLabelOrder());
        final DataWF<ITerm, CEqual> dataWF = new ResolveDataWF(isComplete3, state, query.filter().getDataWF(), query);
        //                lengths = length((IListTerm) query.resultTerm(), state.unifier()).map(ImmutableList::of)
        //                        .orElse(ImmutableList.of(0, 1, 2, -1));

        // @formatter:off
        final NameResolution<Scope, ITerm, ITerm, CEqual> nameResolution = new NameResolution<>(
                state.scopeGraph(), query.relation(),
                labelWF, labelOrd, isComplete2,
                dataWF, isAlways, isComplete2);
        // @formatter:on

        try {
            nameResolution.resolve(scope, () -> false);
        } catch(ResolutionException e) {
            return Stream.empty();
        } catch(InterruptedException e) {
            throw new MetaborgRuntimeException(e);
        }

        return Stream.of(new SearchNode<>(ctx.nextNodeId(), input, parent, parent.desc()));
    }

    @Override public String toString() {
        return "can-resolve";
    }

}