package org.metaborg.meta.nabl2.solver.messages;

import java.io.Serializable;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageContent;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.constraints.messages.MessageKind;
import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Set;

public class Messages implements IMessages, Serializable {
    private static final long serialVersionUID = 42L;

    private final Set.Immutable<IMessageInfo> all;
    private final Set.Immutable<IMessageInfo> errors;
    private final Set.Immutable<IMessageInfo> warnings;
    private final Set.Immutable<IMessageInfo> notes;

    public Messages(Set.Immutable<IMessageInfo> messages) {
        this.all = messages;
        this.errors = Set.Immutable.<IMessageInfo>of().__insertAll(
                messages.stream().filter(m -> m.getKind().equals(MessageKind.ERROR)).collect(Collectors.toSet()));
        this.warnings = Set.Immutable.<IMessageInfo>of().__insertAll(
                messages.stream().filter(m -> m.getKind().equals(MessageKind.WARNING)).collect(Collectors.toSet()));
        this.notes = Set.Immutable.<IMessageInfo>of().__insertAll(
                messages.stream().filter(m -> m.getKind().equals(MessageKind.NOTE)).collect(Collectors.toSet()));
    }

    public boolean add(IMessageInfo message) {
        switch(message.getKind()) {
            default:
            case ERROR:
                return add(message, errors);
            case WARNING:
                return add(message, warnings);
            case NOTE:
                return add(message, notes);
        }
    }

    public boolean addAll(Iterable<? extends IMessageInfo> messages) {
        boolean changed = false;
        for(IMessageInfo message : messages) {
            changed |= add(message);
        }
        return changed;
    }

    public boolean addAll(IMessages messages) {
        return addAll(messages.getAll());
    }

    private boolean add(IMessageInfo message, Set<IMessageInfo> collection) {
        if(collection.add(message)) {
            all.add(message);
            return true;
        }
        return false;
    }

    @Override public Set.Immutable<IMessageInfo> getAll() {
        return all;
    }

    @Override public Set.Immutable<IMessageInfo> getErrors() {
        return errors;
    }

    @Override public Set.Immutable<IMessageInfo> getWarnings() {
        return warnings;
    }

    @Override public Set.Immutable<IMessageInfo> getNotes() {
        return notes;
    }

    public static class Builder implements IMessages.Builder {

        private final Set.Transient<IMessageInfo> messages;

        public Builder() {
            this.messages = Set.Transient.of();
        }

        public Builder(IMessages messages) {
            this.messages = messages.getAll().asTransient();
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

        public void merge(IMessages other) {
            messages.__insertAll(other.getAll());
        }

        public IMessages build() {
            return new Messages(messages.freeze());
        }

    }

    public static java.util.Set<IMessageInfo> unsolvedErrors(Iterable<? extends IConstraint> constraints) {
        return Iterables2.stream(constraints).map(c -> {
            IMessageContent content = MessageContent.builder().append("Unsolved: ").append(c.pp()).build();
            return c.getMessageInfo().withDefaultContent(content);
        }).collect(Collectors.toSet());
    }

}