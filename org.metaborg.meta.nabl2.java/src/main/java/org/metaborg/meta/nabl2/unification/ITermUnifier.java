package org.metaborg.meta.nabl2.unification;

import java.util.Set;

import org.metaborg.meta.nabl2.collections.Throws;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;

public interface ITermUnifier {

    Throws<? extends IUnifyResult,UnificationException> unify(ITerm term1, ITerm term2);

    IFindResult find(ITerm term);

    Set<ITermVar> variables();

}