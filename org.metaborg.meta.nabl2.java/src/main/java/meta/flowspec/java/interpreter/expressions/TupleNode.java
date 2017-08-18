package meta.flowspec.java.interpreter.expressions;

import java.util.Arrays;

import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.StrategoList;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;

import meta.flowspec.java.interpreter.values.Tuple;

public class TupleNode extends ExpressionNode {
    @Children
    private final ExpressionNode[] children;

    public TupleNode(ExpressionNode[] children) {
        super();
        this.children = children;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object[] childVals = Arrays.stream(children).map(c -> c.executeGeneric(frame)).toArray();
        return new Tuple(childVals);
    }

    public static TupleNode fromIStrategoAppl(IStrategoAppl appl, FrameDescriptor frameDescriptor) {
        IStrategoTerm first = appl.getSubterm(0);
        IStrategoList others = Tools.listAt(appl, 1);
        IStrategoTerm[] children = new StrategoList(first, others, null, IStrategoTerm.SHARABLE).getAllSubterms();
        return new TupleNode(Arrays.stream(children).map(c ->
            ExpressionNode.fromIStrategoTerm(c, frameDescriptor)).toArray(ExpressionNode[]::new));
    }
}
