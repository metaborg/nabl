package mb.statix.constraints.messages;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.solver.IConstraint;

public class MessageUtil {

    // @formatter:off
    private static Map<Class<? extends IConstraint>, MessageKind> KINDS =
        ImmutableMap.<Class<? extends IConstraint>, MessageKind>builder()
            .put(CAstId.class, MessageKind.WARNING)
            .put(CAstProperty.class, MessageKind.WARNING)
            .build();
    // @formatter:on

    public static MessageKind defaultMessageKind(IConstraint c) {
        return KINDS.getOrDefault(c.getClass(), MessageKind.ERROR);
    }

    public static IMessage findClosestMessage(IConstraint c) {
        return findClosestMessage(c, KINDS.getOrDefault(c.getClass(), MessageKind.ERROR));
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
