package mb.statix.generator.strategy;

import java.util.HashMap;
import java.util.Optional;

import org.metaborg.util.functions.Predicate2;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.IncompleteException;
import mb.scopegraph.oopsla20.reference.LabelOrder;
import mb.scopegraph.oopsla20.reference.LabelWF;
import mb.scopegraph.oopsla20.reference.RegExpLabelWF;
import mb.scopegraph.oopsla20.reference.RelationLabelOrder;
import mb.scopegraph.oopsla20.reference.ResolutionException;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CResolveQuery;
import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;
import mb.statix.generator.scopegraph.DataWF;
import mb.statix.generator.scopegraph.NameResolution;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.spec.Spec;

public final class DelayStuckQueries extends SearchStrategy<SearchState, SearchState> {

    @Override protected SearchNodes<SearchState> doApply(SearchContext ctx, SearchNode<SearchState> node) {
        final SearchState input = node.output();
        final IState.Immutable state = input.state();
        final ICompleteness.Immutable completeness = input.completeness();

        final java.util.Map<IConstraint, Delay> delays = new HashMap<>();
        input.constraints().stream().filter(c -> c instanceof CResolveQuery).map(c -> (CResolveQuery) c)
                .forEach(q -> checkDelay(ctx.spec(), q, state, completeness).ifPresent(d -> delays.put(q, d)));

        final SearchState newState = input.delay(delays.entrySet());
        final String desc = this.toString() + "[" + delays.size() + "]";
        return SearchNodes.of(node, this::toString, new SearchNode<>(ctx.nextNodeId(), newState, node, desc));
    }

    private Optional<Delay> checkDelay(Spec spec, CResolveQuery query, IState.Immutable state,
            ICompleteness.Immutable completeness) {
        final IUniDisunifier unifier = state.unifier();

        if(!unifier.isGround(query.scopeTerm())) {
            return Optional.of(Delay.ofVars(unifier.getVars(query.scopeTerm())));
        }
        final Scope scope = Scope.matcher().match(query.scopeTerm(), unifier).orElse(null);
        if(scope == null) {
            return Optional.empty();
        }

        final Boolean isAlways;
        try {
            isAlways = query.min().getDataEquiv().isAlways().orElse(null);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        if(isAlways == null) {
            return Optional.empty();
        }

        final LabelWF<ITerm> labelWF = RegExpLabelWF.of(query.filter().getLabelWF());
        final LabelOrder<ITerm> labelOrd = new RelationLabelOrder<>(query.min().getLabelOrder());
        final DataWF<ITerm, CEqual> dataWF = new ResolveDataWF(state, completeness, query.filter().getDataWF(), query);
        final Predicate2<Scope, EdgeOrData<ITerm>> isComplete =
                (s, l) -> completeness.isComplete(s, l, state.unifier());

        // @formatter:off
        final NameResolution<Scope, ITerm, ITerm, CEqual> nameResolution = new NameResolution<>(
                spec,
                state.scopeGraph(),
                spec.allLabels(),
                labelWF, labelOrd, 
                dataWF, isAlways, isComplete);
        // @formatter:on

        try {
            nameResolution.resolve(scope, () -> false);
        } catch(IncompleteException e) {
            return Optional.of(Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.label())));
        } catch(ResolutionException e) {
            throw new RuntimeException("Unexpected resolution exception.", e);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    @Override public String toString() {
        return "delay-stuck-queries";
    }

}