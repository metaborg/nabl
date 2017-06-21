package org.metaborg.meta.nabl2.unification;

import java.util.Set;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.util.functions.Function1;

public interface IUnifier {

    Set<ITermVar> getAllVars();

    ITerm find(ITerm t);

    Stream<Tuple2<ITermVar, ITerm>> stream();

    interface Transient extends IUnifier {

        UnificationResult unify(ITerm left, ITerm right) throws UnificationException;

        boolean putAll(IUnifier other);

        boolean map(Function1<ITerm, ITerm> mapper) throws UnificationException;

        IUnifier.Immutable freeze();

    }

    interface Immutable extends IUnifier {

        IUnifier.Transient melt();

    }

}