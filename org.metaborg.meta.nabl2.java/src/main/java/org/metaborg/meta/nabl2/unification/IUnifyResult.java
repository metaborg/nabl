package org.metaborg.meta.nabl2.unification;

import org.metaborg.meta.nabl2.terms.generic.TermPair;

public interface IUnifyResult {

    ITermUnifier unifier();

    Iterable<? extends TermPair> defers();

}