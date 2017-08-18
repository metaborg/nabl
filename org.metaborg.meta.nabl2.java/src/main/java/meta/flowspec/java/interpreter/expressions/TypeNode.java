package meta.flowspec.java.interpreter.expressions;

import com.oracle.truffle.api.frame.VirtualFrame;

import meta.flowspec.java.interpreter.locals.ReadVarNode;

public class TypeNode extends ExpressionNode {
    @SuppressWarnings("unused")
    private final ReadVarNode occurence;

    public TypeNode(ReadVarNode occurence) {
        super();
        this.occurence = occurence;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("Getting the type of an occurence is currently unimplemented");
    }
}
