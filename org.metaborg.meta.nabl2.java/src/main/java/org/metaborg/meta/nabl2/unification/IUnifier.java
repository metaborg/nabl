package org.metaborg.meta.nabl2.unification;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;

public interface IUnifier {

    Iterable<ITermVar> getAllVars();
    
    ITerm find(ITerm t);

}