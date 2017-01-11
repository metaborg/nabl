package org.metaborg.meta.nabl2.solver;

import java.util.Optional;

import org.metaborg.meta.nabl2.terms.ITerm;

public interface IProperties<T> {

    Iterable<T> getIndices();

    Iterable<ITerm> getDefinedKeys(T index);

    Optional<ITerm> getValue(T index, ITerm key);

}