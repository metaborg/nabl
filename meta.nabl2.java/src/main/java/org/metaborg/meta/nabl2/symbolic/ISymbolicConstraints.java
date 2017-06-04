package org.metaborg.meta.nabl2.symbolic;

import org.metaborg.meta.nabl2.terms.ITerm;

import io.usethesource.capsule.Set;

public interface ISymbolicConstraints {

    Set.Immutable<ITerm> getFacts();

    Set.Immutable<ITerm> getGoals();

}