package mb.statix.solver.constraint;

import java.util.Collections;
import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.Completeness;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.ConstraintResult;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;
import mb.statix.taico.solver.ModuleSolver;

public class CModule implements IConstraint {

    private final @Nullable IConstraint cause;
    private Iterable<IConstraint> constraints;
    private State state;
    
    @Deprecated
    public CModule(State state, Iterable<IConstraint> constraints) {
        this(state, constraints, null);
    }
    
    public CModule(State state, Iterable<IConstraint> constraints, @Nullable IConstraint cause) {
        this.constraints = constraints;
        this.cause = cause;
        this.state = state;
    }
    
    @Override
    public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }
    
    @Override
    public CModule withCause(@Nullable IConstraint cause) {
        return new CModule(state, constraints, cause);
    }
    
    // TODO TAICO check if we need to take care of critical edges
    
    @Override
    public CModule apply(ISubstitution.Immutable subst) {
        //TODO TAICO we don't need to do anything here since this crosses a module boundary?
        System.out.println("[MODULE] CModule constraint on which a substitution is being applied: " + subst);
        return new CModule(state, constraints, cause);
    }

    @Override
    public Optional<ConstraintResult> solve(State oldState, ConstraintContext params) throws InterruptedException, Delay {
        ModuleSolver solver = new ModuleSolver(this.state, constraints, new Completeness(), params.debug());
        
        //TODO proxy debug and commit?
        
        //TODO does the state change with the results of this constraint?
        params.debug().warn("Solving module constraint {}", this);
        //TODO store solver state to as far as it gets.
        //TODO Solver state is important since we might be only able to solve this partially, with mutual dependencies with other modules
        //In addition, a significant amount of work might already be solved by this solver, so the intermediate states need to be tracked better
        //An idea is to return a new CModule constraint with less inner constraints, so that outer solvers know that progress is being made
        solver.solve();
        params.debug().warn("Solved module constraint {}", this);
        return Optional.of(ConstraintResult.ofConstraints(oldState, Collections.emptyList()));
    }

    @Override
    public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("module{");
        if (cause != null) {
            sb.append(cause.toString(termToString));
        }
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return toString(ITerm::toString);
    }

}
