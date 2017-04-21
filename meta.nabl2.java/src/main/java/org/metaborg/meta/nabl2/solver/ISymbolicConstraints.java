package org.metaborg.meta.nabl2.solver;

import java.util.Set;

import org.metaborg.meta.nabl2.terms.ITerm;

public interface ISymbolicConstraints {

    Set<ITerm> getFacts();

    Set<ITerm> getGoals();

}
