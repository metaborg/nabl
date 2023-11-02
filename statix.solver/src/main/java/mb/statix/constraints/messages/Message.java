package mb.statix.constraints.messages;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.completeness.ICompleteness;

public class Message implements IMessage, Serializable {

    private static final long serialVersionUID = 1L;

    private final MessageKind kind;
    private final List<IMessagePart> content;
    private final @Nullable ITerm origin;


    public Message(MessageKind kind) {
        this(kind, ImList.Immutable.of(), null);
    }

    public Message(MessageKind kind, Iterable<IMessagePart> content, @Nullable ITerm origin) {
        this.kind = kind;
        this.content = ImList.Immutable.copyOf(content);
        this.origin = origin;
    }

    @Override public MessageKind kind() {
        return kind;
    }

    @Override public String toString(TermFormatter formatter, Function0<String> getDefaultMessage,
            Function1<ICompleteness.Immutable, String> formatCompleteness) {
        return content.isEmpty() ? getDefaultMessage.apply()
                : content.stream().map(p -> p.toString(formatter)).collect(Collectors.joining());
    }

    @Override public Optional<ITerm> origin() {
        return Optional.ofNullable(origin);
    }

    @Override public void visitVars(Action1<ITermVar> onVar) {
        content.forEach(p -> p.visitVars(onVar));
    }

    @Override public IMessage apply(ISubstitution.Immutable subst) {
        final List<IMessagePart> newContent =
                content.stream().map(p -> p.apply(subst)).collect(ImList.Immutable.toImmutableList());
        final ITerm newOrigin = origin != null ? subst.apply(origin) : null;
        return new Message(kind, newContent, newOrigin);
    }

    @Override public IMessage apply(IRenaming subst) {
        final List<IMessagePart> newContent =
                content.stream().map(p -> p.apply(subst)).collect(ImList.Immutable.toImmutableList());
        final ITerm newOrigin = origin != null ? subst.apply(origin) : null;
        return new Message(kind, newContent, newOrigin);
    }

    @Override public IMessage withKind(MessageKind kind) {
        return new Message(kind, content, origin);
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(kind);
        sb.append(" $[");
        content.forEach(sb::append);
        sb.append("]");
        return sb.toString();
    }

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        Message message = (Message) o;
        return kind == message.kind && Objects.equals(content, message.content)
                && Objects.equals(origin, message.origin);
    }

    @Override public int hashCode() {
        return Objects.hash(kind, content, origin);
    }
}
