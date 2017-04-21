package org.metaborg.meta.nabl2.solver;

import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.terms.ITerm;

public interface IProperties<T> {

    Set<T> getIndices();

    Set<ITerm> getDefinedKeys(T index);

    Optional<ITerm> getValue(T index, ITerm key);

}