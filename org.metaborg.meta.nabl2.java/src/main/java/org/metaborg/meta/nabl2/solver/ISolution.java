package org.metaborg.meta.nabl2.solver;

import java.io.Serializable;

public interface ISolution extends Serializable {

    Iterable<Message> getErrors();

    Iterable<Message> getWarnings();

    Iterable<Message> getNotes();

}