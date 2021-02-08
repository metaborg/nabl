package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.spoofax.StatixTerms.explicateVars;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IState;
import mb.statix.solver.persistent.SolverResult;

public class STX_get_scopegraph extends StatixPrimitive {

    @Inject public STX_get_scopegraph() {
        super(STX_get_scopegraph.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        // @formatter:off
        final List<SolverResult> analyses = M.cases(
            M.blobValue(SolverResult.class).map(ImmutableList::of),
            M.listElems(M.blobValue(SolverResult.class))
        ).match(term).orElseThrow(() -> new InterpreterException("Expected solver result."));
        // @formatter:on

        final Map<Scope, ITerm> dataEntries = Maps.newHashMap(); // Scope * ITerm
        final Map<Scope, ListMultimap<ITerm, Scope>> edgeEntries = Maps.newHashMap(); // Scope * (Label * Scope)
        for(SolverResult analysis : analyses) {
            addScopeEntries(analysis, dataEntries, edgeEntries);
        }

        final List<ITerm> scopeEntries = Lists.newArrayList(); // [Scope * ITerm? * [Label * [Scope]]]
        for(Scope scope : Sets.union(edgeEntries.keySet(), dataEntries.keySet())) {
            final ITerm data = Optional.ofNullable(dataEntries.get(scope)).map(d -> B.newAppl("Some", d))
                    .orElse(B.newAppl("None"));

            final ITerm edges = Optional.ofNullable(edgeEntries.get(scope)).map(es -> {
                final List<ITerm> lblTgts = Lists.newArrayList();
                es.asMap().entrySet().forEach(ee -> {
                    final ITerm lbl_tgt = B.newTuple(ee.getKey(), B.newList(explicateVars(ee.getValue())));
                    lblTgts.add(lbl_tgt);
                });
                return B.newList(lblTgts);
            }).orElse(B.newList());

            scopeEntries.add(B.newTuple(explicateVars(scope), data, edges));
        }

        // @formatter:on
        return Optional.of(B.newList(scopeEntries));
    }

    private void addScopeEntries(SolverResult analysis, Map<Scope, ITerm> dataEntries,
            Map<Scope, ListMultimap<ITerm, Scope>> edgeEntries) {
        final IState.Immutable state = analysis.state();
        final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph = state.scopeGraph();
        final IUniDisunifier.Immutable unifier = state.unifier();

        scopeGraph.getData().forEach((s, d) -> {
            d = unifier.findRecursive(d);
            dataEntries.put(s, d);
        });

        scopeGraph.getEdges().forEach((src_lbl, tgt) -> {
            ListMultimap<ITerm, Scope> edges = edgeEntries.getOrDefault(src_lbl.getKey(), LinkedListMultimap.create());
            edges.putAll(src_lbl.getValue(), tgt);
            edgeEntries.put(src_lbl.getKey(), edges);
        });
    }

}