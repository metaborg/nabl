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

    
    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + all.hashCode();
        result = prime * result + errors.hashCode();
        result = prime * result + notes.hashCode();
        result = prime * result + warnings.hashCode();
        return result;
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        final Messages other = (Messages) obj;
        if(!all.equals(other.all))
            return false;
        if(!errors.equals(other.errors))
            return false;
        if(!notes.equals(other.notes))
            return false;
        if(!warnings.equals(other.warnings))
            return false;
        return true;
    }
    
}