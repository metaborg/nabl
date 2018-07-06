package mb.nabl2.spoofax.analysis;

import java.util.List;
import java.util.Optional;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;

public interface IResult {

    boolean partial();

    List<IConstraint> constraints();

    ISolution solution();

    IResult withSolution(ISolution solution);

    Optional<ITerm> customAnalysis();

    IResult withCustomAnalysis(ITerm term);

}
