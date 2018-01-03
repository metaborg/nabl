package org.metaborg.meta.nabl2.controlflow.terms;

import org.metaborg.meta.nabl2.terms.ITerm;

public interface ICFGNode extends ITerm {

    String getResource();

    String getName();

    Kind getKind();

    enum Kind {
        Normal,
        Start,
        End,
        Artificial;
    }
}