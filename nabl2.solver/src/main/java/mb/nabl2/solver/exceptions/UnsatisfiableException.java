package mb.nabl2.solver.exceptions;

import java.util.Arrays;

import org.metaborg.util.collection.ImList;

import mb.nabl2.constraints.messages.IMessageInfo;

/**
 * Exception thrown if some constraints cannot be satisfied.
 */
public class UnsatisfiableException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ImList.Immutable<IMessageInfo> messages;

    public UnsatisfiableException(IMessageInfo... messages) {
        this(null, Arrays.asList(messages));
    }

    public UnsatisfiableException(Throwable cause, IMessageInfo... messages) {
        this(cause, Arrays.asList(messages));
    }

    public UnsatisfiableException(Iterable<IMessageInfo> messages) {
        this(null, messages);
    }

    public UnsatisfiableException(Throwable cause, Iterable<IMessageInfo> messages) {
        super("", cause);
        this.messages = ImList.Immutable.copyOf(messages);
        assert !this.messages.isEmpty();
    }

    public ImList.Immutable<IMessageInfo> getMessages() {
        return messages;
    }

    @Override public String getMessage() {
        StringBuilder sb = new StringBuilder();
        messages.stream().forEach(m -> {
            sb.append(m.toString());
            sb.append("\n");
        });
        return sb.toString();
    }

}