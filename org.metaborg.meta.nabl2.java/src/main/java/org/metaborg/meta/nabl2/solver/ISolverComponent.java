package org.metaborg.meta.nabl2.solver;

import java.io.Serializable;

import org.metaborg.meta.nabl2.collections.Unit;
import org.metaborg.meta.nabl2.constraints.IConstraint;

public interface ISolverComponent<C extends IConstraint> extends Serializable {

    /**
     * Add a constraint to the constraint set. Solving can be eagerly done,
     * instead of waiting for iterate calls.
     *
     * @param constraint
     * @throws UnsatisfiableException
     */
    Unit add(C constraint) throws UnsatisfiableException;

    /**
     * Try to make progress in solving the constraints.
     *
     * @return Returns true if any progress was made.
     * @throws UnsatisfiableException
     */
    boolean iterate() throws UnsatisfiableException;

    /**
     * Called when none of the solver components can make any more progress. Can
     * be used for final checks, or errors on unsolved constraints.
     * 
     * @throws UnsatisfiableException
     */
    void finish() throws UnsatisfiableException;

}