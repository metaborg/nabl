package org.metaborg.meta.nabl2.solver.messages;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;

public class EmptyMessages implements IMessages, Serializable {

    private static final long serialVersionUID = 42L;

    @Override public Set<IMessageInfo> getErrors() {
        return Collections.emptySet();
    }

    @Override public Set<IMessageInfo> getWarnings() {
        return Collections.emptySet();
    }

    @Override public Set<IMessageInfo> getNotes() {
        return Collections.emptySet();
    }

    @Override public Set<IMessageInfo> getAll() {
        return Collections.emptySet();
    }

    @Override public int hashCode() {
        return 0;
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        return true;
    }

}
