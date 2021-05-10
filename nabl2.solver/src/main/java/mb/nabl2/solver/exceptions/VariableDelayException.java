package mb.nabl2.solver.exceptions;

import java.util.Set;

import org.metaborg.util.collection.CapsuleUtil;

import mb.nabl2.terms.ITermVar;

public class VariableDelayException extends DelayException {

    private static final long serialVersionUID = 42L;

    private final Set<ITermVar> variables;

    public VariableDelayException(Iterable<ITermVar> variables) {
        this.variables = CapsuleUtil.toSet(variables);
        if(this.variables.isEmpty()) {
            throw new IllegalArgumentException("Variables cannot be empty.");
        }
    }

    public Set<ITermVar> variables() {
        return variables;
    }

}