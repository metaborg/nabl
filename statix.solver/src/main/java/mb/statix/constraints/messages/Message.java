package mb.statix.constraints.messages;

import java.io.Serializable;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;

public class Message implements IMessage, Serializable {

    private static final long serialVersionUID = 1L;

    private final MessageKind kind;
    private final @Nullable ITerm origin;

    public Message(MessageKind kind, @Nullable ITerm origin) {
        this.kind = kind;
        this.origin = origin;
    }

    @Override public MessageKind kind() {
        return kind;
    }

    @Override public Optional<ITerm> origin() {
        return Optional.ofNullable(origin);
    }

    @Override public IMessage apply(ISubstitution.Immutable subst) {
        return new Message(kind, origin != null ? subst.apply(origin) : null);
    }

}