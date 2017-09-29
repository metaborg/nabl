package org.metaborg.meta.nabl2.unification;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;

public interface ISubstitution {

    Set<ITermVar> keySet();

    boolean contains(ITermVar var);

    Optional<ITerm> get(ITermVar var);

    boolean isEmpty();

    ITerm find(ITerm term);
    
    interface Immutable extends ISubstitution {

        Immutable put(ITermVar var, ITerm term);

        Immutable putAll(Map<ITermVar, ITerm> entries);

        Immutable remove(ITermVar var);

        Immutable removeAll(Iterable<ITermVar> vars);

        Transient melt();

    }

    interface Transient extends ISubstitution {

        boolean put(ITermVar var, ITerm term);

        boolean putAll(Map<ITermVar, ITerm> entries);

        boolean remove(ITermVar var);

        boolean removeAll(Iterable<ITermVar> vars);

        Immutable freeze();

    }

}