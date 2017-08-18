package meta.flowspec.java.interpreter.expressions;

import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;

import meta.flowspec.java.interpreter.patterns.FunPattern;

public class AbstractionNode extends ExpressionNode {
    @CompilationFinal
    private FunPattern pattern;
    @Child
    private ExpressionNode body;

    public AbstractionNode(FunPattern pattern, ExpressionNode body) {
        this.pattern = pattern;
        this.body = body;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
//        VirtualFrame closureFrame = (VirtualFrame) ((Object) frame).clone();
        // TODO Auto-generated method stub
        return null;
    }

    public static AbstractionNode fromIStrategoAppl(IStrategoAppl appl, FrameDescriptor frameDescriptor) {
        FrameDescriptor closureFrameDescriptor = frameDescriptor.copy();
        return
            new AbstractionNode(
                FunPattern.fromIStrategoAppl(Tools.applAt(appl, 0), closureFrameDescriptor), 
                ExpressionNode.fromIStrategoTerm(appl.getSubterm(1), closureFrameDescriptor));
    }
}
