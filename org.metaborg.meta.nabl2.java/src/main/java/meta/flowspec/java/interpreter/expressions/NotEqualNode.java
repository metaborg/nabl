package meta.flowspec.java.interpreter.expressions;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;

import java.util.Objects;

import meta.flowspec.java.interpreter.expressions.NotEqualNodeGen;
import org.spoofax.interpreter.terms.IStrategoAppl;

@NodeChildren({@NodeChild("left"), @NodeChild("right")})
public abstract class NotEqualNode extends ExpressionNode {
    @Specialization
    protected boolean nequal(int left, int right) {
        return left != right;
    }

    @Specialization
    protected boolean nequal(boolean left, boolean right) {
        return left != right;
    }

    @Specialization
    protected boolean nequal(String left, String right) {
        return !Objects.equals(left, right);
    }

    public static NotEqualNode fromIStrategoAppl(IStrategoAppl appl, FrameDescriptor frameDescriptor) {
        return
            NotEqualNodeGen.create(
                ExpressionNode.fromIStrategoTerm(appl.getSubterm(0), frameDescriptor),
                ExpressionNode.fromIStrategoTerm(appl.getSubterm(1), frameDescriptor));
    }
}
