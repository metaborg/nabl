package org.metaborg.meta.nabl2.solver;

import java.util.Arrays;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;

import com.google.common.collect.ImmutableList;

/**
 * Exception thrown if some constraints cannot be satisfied.
 */
public class UnsatisfiableException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ImmutableList<IMessageInfo> messages;

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
        this.messages = ImmutableList.copyOf(messages);
        assert !this.messages.isEmpty();
    }

    public ImmutableList<IMessageInfo> getMessages() {
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