package mb.statix.concurrent;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.IResult;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.persistent.SolverResult;


public interface IStatixResult extends IResult<Scope, ITerm, ITerm> {

    @Nullable SolverResult solveResult();

    @Nullable Throwable exception();

    default ITerm getExternalRepresentation(ITerm datum) {
        final SolverResult result = solveResult();
        if(result != null) {
            return result.state().unifier().findRecursive(datum);
        }
        return datum;
    }

}