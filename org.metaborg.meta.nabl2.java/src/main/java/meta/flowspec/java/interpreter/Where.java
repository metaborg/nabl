package meta.flowspec.java.interpreter;

import java.util.Arrays;

import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import meta.flowspec.java.interpreter.expressions.ExpressionNode;
import meta.flowspec.java.interpreter.locals.WriteVarNode;

@TypeSystemReference(Types.class)
public class Where extends Node {
    private final WriteVarNode[] bindings;
    private final ExpressionNode body;

    public Where(WriteVarNode[] bindings, ExpressionNode body) {
        super();
        this.bindings = bindings;
        this.body = body;
    }

    public Object execute(VirtualFrame frame) {
        for (WriteVarNode binding : bindings) {
            binding.execute(frame);
        }
        return body.executeGeneric(frame);
    }

    public static Where fromIStrategoTerm(IStrategoTerm term, FrameDescriptor frameDescriptor) {
        assert term instanceof IStrategoAppl : "Expected a constructor application term";
        final IStrategoAppl appl = (IStrategoAppl) term;
        switch (appl.getConstructor().getName()) {
            case "Where" : {
                assert appl.getSubtermCount() == 2 : "Expected TransferFunction to have 2 children";
                IStrategoTerm[] bindings = Tools.listAt(appl, 0).getAllSubterms();
                WriteVarNode[] writeVars =
                    Arrays.stream(bindings).map(t -> WriteVarNode.fromIStrategoTerm(t, frameDescriptor)).toArray(WriteVarNode[]::new);
                ExpressionNode body = ExpressionNode.fromIStrategoTerm(appl.getSubterm(1), frameDescriptor);
                return new Where(writeVars, body);
            }
            default : throw new IllegalArgumentException("Expected constructor TransferFunction");
        }
    }
}
