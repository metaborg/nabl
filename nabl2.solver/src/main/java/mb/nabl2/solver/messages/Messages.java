package mb.nabl2.solver.messages;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.iterators.Iterables2;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;

public abstract class Messages implements IMessages {

    protected Messages() {
    }

    public static class Immutable extends Messages implements IMessages.Immutable, Serializable {
        private static final long serialVersionUID = 42L;

        private final ImList.Immutable<IMessageInfo> messages;

        private Immutable(ImList.Immutable<IMessageInfo> messages) {
            this.messages = messages;
        }

        @Override public List<IMessageInfo> getAll() {
            return messages;
        }

        @Override public Messages.Transient melt() {
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
            return new Messages.Immutable(ImList.Immutable.of());
        }

        @Override public String toString() {
            return messages.toString();
        }

    }

    public static class Transient extends Messages implements IMessages.Transient {

        private final ImList.Transient<IMessageInfo> messages;

        private Transient(ImList.Transient<IMessageInfo> messages) {
            this.messages = messages;
        }

        @Override public boolean add(IMessageInfo message) {
            messages.add(message);
            return true;
        }

        @Override public boolean addAll(Iterable<? extends IMessageInfo> messages) {
            boolean change = false;
            for(IMessageInfo message : messages) {
                this.messages.add(message);
                change |= true;
            }
            return change;
        }

        @Override public boolean addAll(IMessages.Immutable other) {
            messages.addAll(other.getAll());
            return !other.getAll().isEmpty();
        }

        @Override public Messages.Immutable freeze() {
            return new Messages.Immutable(messages.freeze());
        }

        public static Messages.Transient of() {
            return new Messages.Transient(ImList.Transient.of());
        }

    }

    public static java.util.Set<IMessageInfo> unsolvedErrors(Iterable<? extends IConstraint> constraints) {
        return Iterables2.stream(constraints).map(c -> {
            final IMessageContent content = MessageContent.builder().append("Unsolved: ")
                    .append(c.getMessageInfo().getContent().withDefault(c.pp())).build();
            return c.getMessageInfo().withContent(content);
        }).collect(Collectors.toSet());
    }

}
