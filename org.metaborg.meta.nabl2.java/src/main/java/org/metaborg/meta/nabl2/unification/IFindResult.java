package org.metaborg.meta.nabl2.unification;

import org.metaborg.meta.nabl2.terms.ITerm;

public interface IFindResult {

    ITerm rep();

    ITermUnifier unifier();

}