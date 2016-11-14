package org.metaborg.meta.nabl2.unification.eager;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IFindResult;

public final class EagerFindResult implements IFindResult {

    private final ITerm rep;
    private final EagerTermUnifier unifier;

    public EagerFindResult(ITerm rep, EagerTermUnifier unifier) {
        this.rep = rep;
        this.unifier = unifier;
    }

    @Override public ITerm rep() {
        return rep;
    }

    @Override public EagerTermUnifier unifier() {
        return unifier;
    }

}