package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.spoofax.StatixTerms.explicate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IState;
import mb.statix.solver.persistent.SolverResult;

public class STX_debug_scopegraph extends StatixPrimitive {

    @Inject public STX_debug_scopegraph() {
        super(STX_debug_scopegraph.class.getSimpleName(), 0);
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
        final Multimap<Scope, ITerm> edgeEntries = HashMultimap.create(); // Scope * [Label * Scope]
        for(SolverResult analysis : analyses) {
            addScopeEntries(analysis, dataEntries, edgeEntries);
        }

        final List<ITerm> scopeEntries = Lists.newArrayList(); // [Scope * ITerm? * [Label * Scope]]
        for(Scope scope : Sets.union(edgeEntries.keySet(), dataEntries.keySet())) {
            final ITerm data = Optional.ofNullable(dataEntries.get(scope)).map(d -> B.newAppl("Some", d))
                    .orElse(B.newAppl("None"));
            final ITerm edges = B.newList(edgeEntries.get(scope));
            scopeEntries.add(B.newTuple(explicate(scope), data, edges));
        }

        // @formatter:on
        return Optional.of(B.newList(scopeEntries));
    }

    private void addScopeEntries(SolverResult analysis, Map<Scope, ITerm> dataEntries,
            Multimap<Scope, ITerm> edgeEntries) {
        final IState.Immutable state = analysis.state();
        final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph = state.scopeGraph();
        final IUniDisunifier.Immutable unifier = state.unifier();

        scopeGraph.getEdges().forEach((src_lbl, tgt) -> {
            final ITerm lbl_tgt = B.newTuple(src_lbl.getValue(), B.newList(explicate(tgt)));
            edgeEntries.put(src_lbl.getKey(), lbl_tgt);
        });

        scopeGraph.getData().forEach((s, d) -> {
            dataEntries.put(s, explicate(unifier.findRecursive(d)));
        });
    }

}