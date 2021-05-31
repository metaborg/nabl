package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;
import mb.scopegraph.pepm16.terms.Occurrence;

public class SG_get_ref_scope extends AnalysisPrimitive {

    public SG_get_ref_scope() {
        super(SG_get_ref_scope.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return Occurrence.matcher().match(term, solution.unifier()).<ITerm>flatMap(ref -> {
            return solution.scopeGraph().getRefs().get(ref).flatMap(Optional::<ITerm>of);
        });
    }

}