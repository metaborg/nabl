package mb.statix.taico.solver;

import java.util.ArrayList;
import java.util.List;

import mb.nabl2.terms.ITermVar;
import mb.statix.solver.IConstraint;

public class MConstraintResult {
    private List<IConstraint> constraints = new ArrayList<>();
    private List<ITermVar> vars = new ArrayList<>();
    
    public MConstraintResult() {}
    
    public MConstraintResult(IConstraint... constraints) {
        if (constraints != null) {
            for (IConstraint constraint : constraints) {
                this.constraints.add(constraint);
            }
        }
    }
    
    public MConstraintResult(Iterable<? extends IConstraint> constraints, Iterable<? extends ITermVar> vars) {
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

    public List<IConstraint> constraints() {
        return this.constraints;
    }

    public List<ITermVar> vars() {
        return this.vars;
    }

    public static MConstraintResult ofVars(Iterable<? extends ITermVar> vars) {
        return new MConstraintResult(null, vars);
    }

    public static MConstraintResult ofConstraints(Iterable<? extends IConstraint> constraints) {
        return new MConstraintResult(constraints, null);
    }

    public static MConstraintResult ofConstraints(IConstraint... constraints) {
        return new MConstraintResult(constraints);
    }
}
