package mb.statix.taico.solver;

import java.util.ArrayList;
import java.util.List;

import mb.nabl2.terms.ITermVar;
import mb.statix.solver.IConstraint;

public class MConstraintResult {
    private final MState state;
    private List<IConstraint> constraints = new ArrayList<>();
    private List<ITermVar> vars = new ArrayList<>();
    
    public MConstraintResult(MState state) {
        this.state = state;
    }
    
    public MConstraintResult(MState state, IConstraint... constraints) {
        this.state = state;
        
        if (constraints != null) {
            for (IConstraint constraint : constraints) {
                this.constraints.add(constraint);
            }
        }
    }
    
    public MConstraintResult(MState state, Iterable<? extends IConstraint> constraints, Iterable<? extends ITermVar> vars) {
        this.state = state;
        if (constraints != null) {
            for (IConstraint constraint : constraints) {
                this.constraints.add(constraint);
            }
        }
        
        if (vars != null) {
            for (ITermVar var : vars) {
                this.vars.add(var);
            }
        }
    }
    
    public MState state() {
        //TODO Remove
        return this.state;
    }

    public List<IConstraint> constraints() {
        return this.constraints;
    }

    public List<ITermVar> vars() {
        return this.vars;
    }
    
    public static MConstraintResult ofVars(MState state, Iterable<? extends ITermVar> vars) {
        return new MConstraintResult(state, null, vars);
    }
    
    public static MConstraintResult ofConstraints(MState state, Iterable<? extends IConstraint> constraints) {
        return new MConstraintResult(state, constraints, null);
    }
    
    public static MConstraintResult ofConstraints(MState state, IConstraint... constraints) {
        return new MConstraintResult(state, constraints);
    }
}
