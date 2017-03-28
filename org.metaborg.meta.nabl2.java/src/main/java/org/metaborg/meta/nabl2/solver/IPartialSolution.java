package org.metaborg.meta.nabl2.solver;

import java.util.Set;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.unification.IUnifier;

public interface IPartialSolution {

    IUnifier getUnifier();

    IMessages getMessages();

    Set<IConstraint> getUnsolvedConstraints();

}