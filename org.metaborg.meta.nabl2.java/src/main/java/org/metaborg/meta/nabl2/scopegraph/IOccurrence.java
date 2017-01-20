package org.metaborg.meta.nabl2.scopegraph;

import org.metaborg.meta.nabl2.terms.ITerm;

public interface IOccurrence {

    INamespace getNamespace();

    ITerm getName();

    IOccurrenceIndex getIndex();

}