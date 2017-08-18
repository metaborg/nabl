package meta.flowspec.java.interpreter.locals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import meta.flowspec.java.interpreter.expressions.ExpressionNode;

@NodeField(name = "slot", type = FrameSlot.class)
public abstract class ReadVarNode extends ExpressionNode {

    protected abstract FrameSlot getSlot();

    @Specialization(guards = "isInt(frame)")
    protected int readInt(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, getSlot());
    }

//    @Specialization(guards = "isLong(frame)")
//    protected long readLong(VirtualFrame frame) {
//        return FrameUtil.getLongSafe(frame, getSlot());
//    }
//    
//    @Specialization(guards = "isFloat(frame)")
//    protected float readFloat(VirtualFrame frame) {
//        return FrameUtil.getFloatSafe(frame, getSlot());
//    }
//    
//    @Specialization(guards = "isDouble(frame)")
//    protected double readDouble(VirtualFrame frame) {
//        return FrameUtil.getDoubleSafe(frame, getSlot());
//    }

    @Specialization(guards = "isBoolean(frame)")
    protected boolean readBoolean(VirtualFrame frame) {
        return FrameUtil.getBooleanSafe(frame, getSlot());
    }

    @Specialization(replaces = {"readInt", /*"readLong", "readFloat", "readDouble",*/ "readBoolean"})
    protected Object readObject(VirtualFrame frame) {

        if (!frame.isObject(getSlot())) {
            CompilerDirectives.transferToInterpreter();
            Object result = frame.getValue(getSlot());
            frame.setObject(getSlot(), result);
            return result;
        }

        return FrameUtil.getObjectSafe(frame, getSlot());
    }

    protected boolean isInt(VirtualFrame frame) {
        return getSlot().getKind() == FrameSlotKind.Int;
    }

//    protected boolean isLong(VirtualFrame frame) {
//        return getSlot().getKind() == FrameSlotKind.Long;
//    }
//
//    protected boolean isFloat(VirtualFrame frame) {
//        return getSlot().getKind() == FrameSlotKind.Float;
//    }
//
//    protected boolean isDouble(VirtualFrame frame) {
//        return getSlot().getKind() == FrameSlotKind.Double;
//    }

    protected boolean isBoolean(VirtualFrame frame) {
        return getSlot().getKind() == FrameSlotKind.Boolean;
    }
    
}