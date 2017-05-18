package org.metaborg.meta.nabl2.solver.messages;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;

import io.usethesource.capsule.Set;

public interface IMessages {

    Set.Immutable<IMessageInfo> getErrors();

    Set.Immutable<IMessageInfo> getWarnings();

    Set.Immutable<IMessageInfo> getNotes();

    Set.Immutable<IMessageInfo> getAll();

    interface Builder {

        boolean add(IMessageInfo message);

        void merge(IMessages other);

        IMessages build();

    }

}