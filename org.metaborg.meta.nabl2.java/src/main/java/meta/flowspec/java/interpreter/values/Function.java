package meta.flowspec.java.interpreter.values;

import com.oracle.truffle.api.CallTarget;

public class Function {
    private final CallTarget callTarget;
    
    public Function(CallTarget callTarget) {
        this.callTarget = callTarget;
    }
    
    public Object call(Object argument) {
        return this.callTarget.call(argument);
    }
}
