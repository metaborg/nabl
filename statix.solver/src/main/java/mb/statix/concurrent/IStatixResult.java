package mb.statix.concurrent;

import jakarta.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.ITypeChecker.IOutput;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.tracer.SolverTracer;


public interface IStatixResult<TR extends SolverTracer.IResult<TR>> extends IOutput<Scope, ITerm, ITerm> {

    @Nullable SolverResult<TR> solveResult();

    @Nullable Throwable exception();

    default ITerm getExternalRepresentation(ITerm datum) {
        final SolverResult<TR> result = solveResult();
        if(result != null) {
            return result.state().unifier().findRecursive(datum);
        }
        return datum;
    }

}
