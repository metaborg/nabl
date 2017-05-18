package org.metaborg.meta.nabl2.solver.messages;

import java.io.Serializable;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;

import io.usethesource.capsule.Set;

public class EmptyMessages implements IMessages, Serializable {

    private static final long serialVersionUID = 42L;

    @Override public Set.Immutable<IMessageInfo> getErrors() {
        return Set.Immutable.of();
    }

    @Override public Set.Immutable<IMessageInfo> getWarnings() {
        return Set.Immutable.of();
    }

    @Override public Set.Immutable<IMessageInfo> getNotes() {
        return Set.Immutable.of();
    }

    @Override public Set.Immutable<IMessageInfo> getAll() {
        return Set.Immutable.of();
    }

}
