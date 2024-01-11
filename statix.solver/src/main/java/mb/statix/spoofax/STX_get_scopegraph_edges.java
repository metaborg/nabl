package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;


import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.IState;
import mb.statix.solver.persistent.SolverResult;

public class STX_get_scopegraph_edges extends StatixPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_get_scopegraph_edges() {
        super(STX_get_scopegraph_edges.class.getSimpleName(), 1);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final SolverResult<?> analysis = M.blobValue(SolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));
        final IState.Immutable state = analysis.state();
        // @formatter:off
        final ITerm edges = M.cases(
            M.tuple2(Scope.matcher(), StatixTerms.label(), (t, s, r) -> {
                reportInvalidEdgeLabel(analysis, r);
                return B.newList(state.scopeGraph().getEdges(s, r));
            })
        ).match(term).orElseThrow(() -> new InterpreterException("Expected scope-label pair."));
        // @formatter:on
        return Optional.of(edges);
    }

}
