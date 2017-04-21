package org.metaborg.meta.nabl2.unification;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;

public class EmptyUnifier implements IUnifier, Serializable {

    private static final long serialVersionUID = 42L;

    @Override public Set<ITermVar> getAllVars() {
        return Collections.emptySet();
    }

    @Override public ITerm find(ITerm t) {
        return t;
    }

}