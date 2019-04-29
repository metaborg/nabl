package mb.statix.taico.solver.context;

import mb.statix.taico.solver.SolverContext;

/**
 * Base class implementation of {@link IContextAware}.
 */
public class AContextAware implements IContextAware {
    protected SolverContext context;
    
    public AContextAware(SolverContext context) {
        setContext(context);
    }

    @Override
    public void setContext(SolverContext context) {
        IContextAware.super.setContext(context);
        this.context = context;
    }

    @Override
    public SolverContext getContext() {
        return context;
    }
}
