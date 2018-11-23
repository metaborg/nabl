package mb.nabl2.solver.messages;

import java.io.Serializable;
import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Set;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;

public abstract class Messages implements IMessages {

    protected Messages() {
    }

    protected abstract Set<IMessageInfo> messages();

    @Override public Set<IMessageInfo> getAll() {
        return messages();
    }

    public static class Immutable extends Messages implements IMessages.Immutable, Serializable {
        private static final long serialVersionUID = 42L;

        private final Set.Immutable<IMessageInfo> messages;

        private Immutable(Set.Immutable<IMessageInfo> messages) {
            this.messages = messages;
        }

        @Override protected Set<IMessageInfo> messages() {
            return messages;
        }

        @Override public Set.Immutable<IMessageInfo> getAll() {
            return messages;
        }

        public Messages.Transient melt() {
            return new Messages.Transient(messages.asTransient());
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + messages.hashCode();
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            final Messages.Immutable other = (Messages.Immutable) obj;
            if(!messages.equals(other.messages))
                return false;
            return true;
        }

        public static Messages.Immutable of() {
            return new Messages.Immutable(Set.Immutable.of());
        }

    }

    public static class Transient extends Messages implements IMessages.Transient {

        private final Set.Transient<IMessageInfo> messages;

        private Transient(Set.Transient<IMessageInfo> messages) {
            this.messages = messages;
        }

        @Override protected Set<IMessageInfo> messages() {
            return messages;
        }

        public boolean add(IMessageInfo message) {
            return messages.__insert(message);
        }

        public boolean addAll(Iterable<? extends IMessageInfo> messages) {
            boolean change = false;
            for(IMessageInfo message : messages) {
                change |= this.messages.__insert(message);
            }
            return change;
        }

        public boolean addAll(IMessages other) {
            return messages.__insertAll(other.getAll());
        }

        public Messages.Immutable freeze() {
            return new Messages.Immutable(messages.freeze());
        }

        public static Messages.Transient of() {
            return new Messages.Transient(Set.Transient.of());
        }

    }

    public static java.util.Set<IMessageInfo> unsolvedErrors(Iterable<? extends IConstraint> constraints) {
        return Iterables2.stream(constraints).map(c -> {
            IMessageContent content = MessageContent.builder().append("Unsolved: ").append(c.pp()).build();
            return c.getMessageInfo().withDefaultContent(content);
        }).collect(Collectors.toSet());
    }

}
