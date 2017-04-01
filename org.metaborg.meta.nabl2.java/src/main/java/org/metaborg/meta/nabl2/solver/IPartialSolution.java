package org.metaborg.meta.nabl2.solver;

import java.util.Set;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;

public interface IPartialSolution {

    /**
     * @return The configuration used for this solution
     */
    SolverConfig getConfig();

    /**
     * @return The terms that are the interface to this solution
     */
    Set<ITerm> getInterface();

    /**
     * @return The unifier
     */
    IUnifier getUnifier();

    /**
     * @return Collection of messages
     */
    IMessages getMessages();

    /**
     * @return Any residual constraints that were not solved
     */
    Set<IConstraint> getResidualConstraints();

}