package org.metaborg.meta.nabl2.scopegraph;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermIndex;

public interface IOccurrence {

    INamespace getNamespace();

    ITerm getName();

    ITermIndex getPosition();

}