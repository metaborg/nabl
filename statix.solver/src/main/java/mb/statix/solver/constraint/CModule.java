package mb.statix.solver.constraint;

import java.util.Optional;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Predicate1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.ConstraintResult;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.ModuleSolver;

public class CModule implements IConstraint {

    private final @Nullable IConstraint cause;
    private ModuleSolver solver;
    
    public CModule(ModuleSolver parent, MState state, Iterable<IConstraint> constraints, Predicate1<ITermVar> isRigid, Predicate1<ITerm> isClosed, @Nullable IConstraint cause) {
        this.cause = cause;
        this.solver = parent.childSolver(state, constraints, isRigid, isClosed);
    }
    
    private CModule(ModuleSolver solver, @Nullable IConstraint cause) {
        this.solver = solver;
        this.cause = cause;
    }
    
    @Override
    public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }
    
    @Override
    public CModule withCause(@Nullable IConstraint cause) {
        return new CModule(solver, cause);
    }
    
    // TODO TAICO check if we need to take care of critical edges
    
    @Override
    public CModule apply(ISubstitution.Immutable subst) {
        //We don't need to do anything here since this crosses a module boundary.
        System.err.println("[MODULE] CModule constraint on which a substitution is being applied: " + subst);
        return this;
    }
    
    @Override
    public Optional<ConstraintResult> solve(State state, ConstraintContext params) throws InterruptedException, Delay {
        throw new UnsupportedOperationException("Unable to solve module constraint without mutable state");
    }

    @Override
    public Optional<MConstraintResult> solveMutable(MState state, ConstraintContext params) throws InterruptedException, Delay {
        return Optional.of(new MConstraintResult(state));
//        if (solver.isDone()) {
//            return Optional.of(new MConstraintResult(state));
//        } else if (solver.hasFailed()) {
//            return Optional.empty();
//        } else {
//            throw Delay.of();
//        }
        
        //TODO store solver state to as far as it gets.
        //TODO Solver state is important since we might be only able to solve this partially, with mutual dependencies with other modules
        //In addition, a significant amount of work might already be solved by this solver, so the intermediate states need to be tracked better
        //An idea is to return a new CModule constraint with less inner constraints, so that outer solvers know that progress is being made
    }

    @Override
    public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("module{");
        sb.append(solver.getOwner().getId());
        
        if (cause != null) {
            sb.append(", ");
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
