package mb.nabl2.solver.messages;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.PrintlineLogger;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageContent;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;

public abstract class Messages implements IMessages {

    protected Messages() {
    }

    public static class Immutable extends Messages implements IMessages.Immutable, Serializable {

        private static final PrintlineLogger log = PrintlineLogger.logger(Messages.Immutable.class);
        private static final long serialVersionUID = 42L;

        private final ImList.Immutable<IMessageInfo> messages;

        private Immutable(ImList.Immutable<IMessageInfo> messages) {
            this.messages = messages;
        }

        @Override public List<IMessageInfo> getAll() {
            return messages;
        }

        @Override public Messages.Transient melt() {
            log.info("- melt {}: {}", System.identityHashCode(this), messages);
            return new Messages.Transient(messages.mutableCopy());
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

        private static final PrintlineLogger log = PrintlineLogger.logger(Messages.Transient.class);

        private final ImList.Mutable<IMessageInfo> messages;

        private Transient(ImList.Mutable<IMessageInfo> messages) {
            this.messages = messages;
        }

        @Override public boolean add(IMessageInfo message) {
            messages.add(message);
            log.info("- added {}: {}", message, messages);
            return true;
        }

        @Override public boolean addAll(Iterable<? extends IMessageInfo> messages) {
            boolean change = false;
            for(IMessageInfo message : messages) {
                change |= add(message);
            }
            return change;
        }

        @Override public boolean addAll(IMessages.Immutable other) {
            return messages.addAll(other.getAll());
        }

        @Override public Messages.Immutable freeze() {
            log.info("- freeze {}: {}", System.identityHashCode(this), messages);
            return new Messages.Immutable(messages.freeze());
        }

        public static Messages.Transient of() {
            return new Messages.Transient(ImList.Mutable.of());
        }

        @Override public String toString() {
            return messages.toString();
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
