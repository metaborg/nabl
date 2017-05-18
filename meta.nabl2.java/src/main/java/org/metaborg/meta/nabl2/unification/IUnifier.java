package org.metaborg.meta.nabl2.unification;

import java.util.Set;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;

public interface IUnifier {

    Set<ITermVar> getAllVars();

    ITerm find(ITerm t);

    interface Transient extends IUnifier {

        UnificationResult unify(ITerm left, ITerm right) throws UnificationException;

        void merge(IUnifier other);

        IUnifier freeze();

    }

}