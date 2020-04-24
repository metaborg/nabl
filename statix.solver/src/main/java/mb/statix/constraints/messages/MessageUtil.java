package mb.statix.constraints.messages;

import mb.statix.solver.IConstraint;

public class MessageUtil {

    /**
     * Find closest message in the
     */
    public static IMessage findClosestMessage(IConstraint c) {
        IMessage message = null;
        while(c != null) {
            IMessage m;
            if((m = c.message().orElse(null)) != null && (message == null || message.kind().isWorseThan(m.kind()))) {
                message = m;
            }
            c = c.cause().orElse(null);
        }
        if(message == null) {
            message = new Message(MessageKind.ERROR);
        }
        return message;
    }

}