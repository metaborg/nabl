package org.metaborg.meta.nabl2.solver.messages;

import java.util.Set;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;

public interface IMessages {

    Set<IMessageInfo> getErrors();

    Set<IMessageInfo> getWarnings();

    Set<IMessageInfo> getNotes();

    Set<IMessageInfo> getAll();

}