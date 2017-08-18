package meta.flowspec.java.interpreter.expressions;

import org.spoofax.interpreter.terms.IStrategoAppl;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;

import meta.flowspec.java.interpreter.patterns.FunPattern;

public class MatchNode extends ExpressionNode {
    @Child
    private ExpressionNode subject;
    @Children
    private FunPattern[] matchArms;
    @Children
    private ExpressionNode[] matchBodies;

    public MatchNode(ExpressionNode subject, FunPattern[] matchArms, ExpressionNode[] matchBodies) {
        this.subject = subject;
        this.matchArms = matchArms;
        this.matchBodies = matchBodies;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // TODO Auto-generated method stub
        return null;
    }

    public MatchNode fromIStrategoAppl(IStrategoAppl appl, FrameDescriptor frameDescriptor) {
     // TODO Auto-generated method stub
        return null;
    }
}
