package mb.statix.taico.solver.context;

import mb.statix.taico.solver.SolverContext;

/**
 * Interface for classes that need access to the current solver context.
 * The solver context will always be the current one used for solving the fragement the observer
 * belongs to.
 */
public interface IContextAware {
    /**
     * Sets the context to the given context and registers this {@link IContextAware} with the
     * given context.
     * <p>
     * Implementing classes should override this method, but keep a call to the super method, e.g.
     * <pre>
     * public void setContext(SolverContext context) {
     *     this.context = context;
     *     IContextAware.super.setContext(context);
     * }
     * </pre>
     * 
     * @param context
     *      the context to use
     */
    public default void setContext(SolverContext context) {
        context.register(this);
    }
    
    /**
     * @return
     *      the current solver context
     */
    public SolverContext getContext();
}
