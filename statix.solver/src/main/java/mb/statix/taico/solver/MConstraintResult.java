package mb.statix.taico.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mb.nabl2.terms.ITermVar;
import mb.statix.solver.IConstraint;

public class MConstraintResult {
    private List<IConstraint> constraints = new ArrayList<>();
    private List<ITermVar> vars = new ArrayList<>();
    private Map<ITermVar, ITermVar> existentials = new HashMap<>();
    
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
    
    public Map<ITermVar, ITermVar> existentials() {
        return existentials;
    }
    
    public MConstraintResult withExistentials(Map<ITermVar, ITermVar> existentials) {
        this.existentials = existentials;
        return this;
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
    
    public static MConstraintResult of() {
        return new MConstraintResult();
    }
}
