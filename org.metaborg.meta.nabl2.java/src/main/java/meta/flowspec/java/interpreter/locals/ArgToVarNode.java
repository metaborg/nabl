package meta.flowspec.java.interpreter.locals;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import meta.flowspec.java.interpreter.Types;

@TypeSystemReference(Types.class)
public class ArgToVarNode extends Node {
    public ArgToVarNode(int argumentOffset, FrameSlot slot) {
        super();
        this.argumentOffset = argumentOffset;
        this.slot = slot;
    }

    private final int argumentOffset;
    private final FrameSlot slot;

    public void execute(VirtualFrame frame) {
        frame.setObject(slot, frame.getArguments()[argumentOffset]);
    }
}
