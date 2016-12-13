package org.metaborg.meta.nabl2.unification;

import org.metaborg.meta.nabl2.terms.ITerm;

public interface IUnifier {

    ITerm find(ITerm t);
    
}