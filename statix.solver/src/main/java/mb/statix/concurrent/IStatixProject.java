package mb.statix.concurrent;

import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.statix.library.IStatixLibrary;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.tracer.SolverTracer;
import mb.statix.spec.Rule;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.impl.Result;

public interface IStatixProject<TR extends SolverTracer.IResult<TR>> {

    String resource();

    /**
     * Rule for the project. Must have type {@literal rule : scope}.
     */
    Optional<Rule> rule();

    /**
     * Direct sub groups of this project.
     */
    Map<String, IStatixGroup> groups();

    /**
     * Direct sub units of this project.
     */
    Map<String, IStatixUnit> units();

    /**
     * Direct sub units of this project.
     */
    Map<String, IStatixLibrary> libraries();

    /**
     * Indicates whether unit was changed since previous run.
     */
    boolean changed();

    /**
     * Result from previous type-checker run.
     */
    @Nullable IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, ProjectResult<TR>, SolverState>> previousResult();

    /**
     * @return Total number of units (including groups and subunits) in the project.
     */
    default int size(int parallellism) {
        return 1 + groups().values().stream().mapToInt(IStatixGroup::size).sum() + units().size()
                + libraries().size() * (parallellism + 1);
    }

}
