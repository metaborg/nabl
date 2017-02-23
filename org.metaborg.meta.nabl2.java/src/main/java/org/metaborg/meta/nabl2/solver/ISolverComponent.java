package org.metaborg.meta.nabl2.solver;

import java.util.Collection;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.util.time.AggregateTimer;

public interface ISolverComponent<C extends IConstraint> {

    /**
     * Return the class of constraints solved by this component.
     * 
     * @return Class of constraints
     */
    Class<C> getConstraintClass();

    /**
     * Add a constraint to the constraint set. Solving can be eagerly done, instead of waiting for iterate calls.
     *
     * @param constraint
     * @throws UnsatisfiableException, InterruptedException
     */
    Unit add(C constraint) throws UnsatisfiableException, InterruptedException;

    /**
     * Try to make progress in solving the constraints.
     *
     * @return Returns true if any progress was made.
     * @throws UnsatisfiableException, InterruptedException
     */
    boolean iterate() throws UnsatisfiableException, InterruptedException;

    /**
     * Called when none of the solver components can make any more progress. Can be used for final checks, but should
     * normally not contribute to inference that can affect other components of the solver.
     * 
     * @return Unsolved constraints
     * @throws InterruptedException
     */
    Iterable<? extends C> finish() throws InterruptedException;

    /**
     * Get the solution of this component as a normalized constraint.
     * 
     * @return Normalized constraints.
     * @throws UnsupportedOperationException
     */
    Collection<C> getNormalizedConstraints(IMessageInfo messageInfo);
    
    AggregateTimer getTimer();

}