package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.solver.ISolution;
import mb.nabl2.spoofax.analysis.IResult;
import mb.nabl2.terms.ITerm;
import mb.scopegraph.pepm16.terms.ScopeGraphTerms;

public class SG_debug_scope_graph extends AnalysisPrimitive {

    public SG_debug_scope_graph() {
        super(SG_debug_scope_graph.class.getSimpleName());
    }

    @Override protected Optional<? extends ITerm> call(IResult result, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if(result.partial()) {
            return Optional.empty();
        }
        final ISolution sol = result.solution();
        return Optional.of(ScopeGraphTerms.build(sol.scopeGraph(), sol.declProperties(), sol.unifier()));
    }

}