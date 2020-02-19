package mb.statix.constraints.messages;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;

public class Message implements IMessage, Serializable {

    private static final long serialVersionUID = 1L;

    private final MessageKind kind;
    private final List<IMessagePart> content;
    private final @Nullable ITerm origin;


    public Message(MessageKind kind) {
        this(kind, ImmutableList.of(), null);
    }

    public Message(MessageKind kind, Iterable<IMessagePart> content, @Nullable ITerm origin) {
        this.kind = kind;
        this.content = ImmutableList.copyOf(content);
        this.origin = origin;
    }

    @Override public MessageKind kind() {
        return kind;
    }

    @Override public String toString(TermFormatter formatter) {
        return content.stream().map(p -> p.toString(formatter)).collect(Collectors.joining());
    }

    @Override public Optional<ITerm> origin() {
        return Optional.ofNullable(origin);
    }

    @Override public Set<ITermVar> boundVars() {
        return ImmutableSet.of();
    }

    @Override public Set<ITermVar> freeVars() {
        final ImmutableSet.Builder<ITermVar> freeVars = ImmutableSet.builder();
        content.forEach(c -> freeVars.addAll(c.freeVars()));
        origin().ifPresent(o -> freeVars.addAll(o.getVars()));
        return freeVars.build();
    }

    @Override public IMessage doSubstitute(IRenaming.Immutable localRenaming, ISubstitution.Immutable totalSubst) {
        final List<IMessagePart> newContent =
                content.stream().map(p -> p.recSubstitute(totalSubst)).collect(ImmutableList.toImmutableList());
        final ITerm newOrigin = origin != null ? totalSubst.apply(origin) : null;
        return new Message(kind, newContent, newOrigin);
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(kind);
        sb.append(" $[");
        content.forEach(sb::append);
        sb.append("]");
        return sb.toString();
    }

}