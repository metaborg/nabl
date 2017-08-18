package meta.flowspec.java.interpreter.patterns;

import org.spoofax.interpreter.terms.IStrategoAppl;

import com.oracle.truffle.api.frame.FrameDescriptor;

public abstract class FunPattern {

    public static FunPattern fromIStrategoAppl(IStrategoAppl appl, FrameDescriptor frameDescriptor) {
        switch(appl.getConstructor().getName()) {
            case "ADTAppl": throw new RuntimeException("unimplemented");
            case "ADTCons": throw new RuntimeException("unimplemented");
            case "Term": throw new RuntimeException("unimplemented");
            case "Wildcard": throw new RuntimeException("unimplemented");
            case "Var": throw new RuntimeException("unimplemented");
            case "At": throw new RuntimeException("unimplemented");
            case "Tuple": throw new RuntimeException("unimplemented");
            case "String": throw new RuntimeException("unimplemented");
            case "Int": throw new RuntimeException("unimplemented");
            default: throw new IllegalArgumentException("Unknown constructor for FunPattern: " + appl.getConstructor().getName());
        }
    }

}
