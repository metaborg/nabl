package mb.statix.constraints.messages;

import mb.statix.solver.IConstraint;

import javax.annotation.Nullable;

public class MessageUtil {

    public static IMessage findClosestMessage(IConstraint c) {
        return findClosestMessage(c, MessageKind.ERROR);
    }

    /**
     * Find closest message in the
     */
    public static IMessage findClosestMessage(IConstraint c, MessageKind kind) {
        @Nullable IMessage message = null;
        while(c != null) {
            @Nullable IMessage m;
            if((m = c.message().orElse(null)) != null && (message == null || message.kind().isWorseThan(m.kind()))) {
                message = m;
            }
            c = c.cause().orElse(null);
        }
        if(message == null) {
            message = new Message(kind);
        }
        return message;
    }

}
