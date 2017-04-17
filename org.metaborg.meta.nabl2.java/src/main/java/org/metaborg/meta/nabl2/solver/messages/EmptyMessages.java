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

}
