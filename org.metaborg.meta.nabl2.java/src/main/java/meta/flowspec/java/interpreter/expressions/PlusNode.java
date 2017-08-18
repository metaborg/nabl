package meta.flowspec.java.interpreter.expressions;

import meta.flowspec.java.interpreter.expressions.PlusNodeGen;
import org.spoofax.interpreter.terms.IStrategoAppl;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;

@NodeChildren({@NodeChild("left"), @NodeChild("right")})
public abstract class PlusNode extends ExpressionNode {
    @Specialization
    protected int equal(int left, int right) {
        return left + right;
    }

    public static PlusNode fromIStrategoAppl(IStrategoAppl appl, FrameDescriptor frameDescriptor) {
        return
            PlusNodeGen.create(
                ExpressionNode.fromIStrategoTerm(appl.getSubterm(0), frameDescriptor),
                ExpressionNode.fromIStrategoTerm(appl.getSubterm(1), frameDescriptor));
    }
}
