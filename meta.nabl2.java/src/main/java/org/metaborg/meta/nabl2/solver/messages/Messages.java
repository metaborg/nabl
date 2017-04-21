package org.metaborg.meta.nabl2.solver.messages;

import java.io.Serializable;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;

import com.google.common.collect.Sets;

public class Messages implements IMessages, Serializable {
    private static final long serialVersionUID = 42L;

    private final Set<IMessageInfo> all;
    private final Set<IMessageInfo> errors;
    private final Set<IMessageInfo> warnings;
    private final Set<IMessageInfo> notes;

    public Messages() {
        this.all = Sets.newHashSet();
        this.errors = Sets.newHashSet();
        this.warnings = Sets.newHashSet();
        this.notes = Sets.newHashSet();
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

    @Override public Set<IMessageInfo> getAll() {
        return all;
    }

    @Override public Set<IMessageInfo> getErrors() {
        return errors;
    }

    @Override public Set<IMessageInfo> getWarnings() {
        return warnings;
    }

    @Override public Set<IMessageInfo> getNotes() {
        return notes;
    }

    public static Messages merge(IMessages... messages) {
        Messages result = new Messages();
        for(IMessages message : messages) {
            result.addAll(message);
        }
        return result;
    }

}