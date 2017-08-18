package meta.flowspec.java.interpreter.expressions;

import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;

import com.oracle.truffle.api.frame.VirtualFrame;

public class StringLiteralNode extends ExpressionNode {
    private final String value;

    public StringLiteralNode(String value) {
        this.value = value;
    }

    @Override
    public String executeGeneric(VirtualFrame frame) {
        return value;
    }

    public static StringLiteralNode fromIStrategoAppl(IStrategoAppl appl) {
        return new StringLiteralNode(Tools.javaStringAt(appl, 0));
    }
}
