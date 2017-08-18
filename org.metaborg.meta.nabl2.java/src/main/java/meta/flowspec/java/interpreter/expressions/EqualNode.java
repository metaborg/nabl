package meta.flowspec.java.interpreter.expressions;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;

import meta.flowspec.java.interpreter.expressions.EqualNodeGen;
import org.spoofax.interpreter.terms.IStrategoAppl;

import java.util.Objects;

@NodeChildren({@NodeChild("left"), @NodeChild("right")})
public abstract class EqualNode extends ExpressionNode {
    @Specialization
    protected boolean equal(int left, int right) {
        return left == right;
    }

    @Specialization
    protected boolean equal(boolean left, boolean right) {
        return left == right;
    }

    @Specialization
    protected boolean equal(String left, String right) {
        return Objects.equals(left, right);
    }

    /**
     * We covered all the cases that can return true in the type specializations above. If we
     * compare two values with different types, the result is known to be false.
     * <p>
     * Note that the guard is essential for correctness: without the guard, the specialization would
     * also match when the left and right value have the same type. The following scenario would
     * return a wrong value: First, the node is executed with the left value 42 (type long) and the
     * right value "abc" (String). This specialization matches, and since it is the first execution
     * it is also the only specialization. Then, the node is executed with the left value "42" (type
     * long) and the right value "42" (type long). Since this specialization is already present, and
     * (without the guard) also matches (long values can be boxed to Object), it is executed. The
     * wrong return value is "false".
     */
    @Specialization(guards = "left.getClass() != right.getClass()")
    protected boolean equal(Object left, Object right) {
        assert !left.equals(right);
        return false;
    }

    public static EqualNode fromIStrategoAppl(IStrategoAppl appl, FrameDescriptor frameDescriptor) {
        return
            EqualNodeGen.create(
                ExpressionNode.fromIStrategoTerm(appl.getSubterm(0), frameDescriptor),
                ExpressionNode.fromIStrategoTerm(appl.getSubterm(1), frameDescriptor));
    }
}
