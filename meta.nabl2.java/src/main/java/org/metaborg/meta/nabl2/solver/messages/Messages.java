package org.metaborg.meta.nabl2.solver.messages;

import java.io.Serializable;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageContent;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Set;

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