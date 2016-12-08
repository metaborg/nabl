package org.metaborg.meta.nabl2.scopegraph;

import org.metaborg.meta.nabl2.terms.ITerm;

public interface ISpacedName {

    INamespace getNamespace();

    ITerm getName();

}