package mb.statix.taico.incremental.manager;

import java.io.Serializable;

import mb.statix.taico.incremental.Flag;
import mb.statix.taico.solver.ModuleSolver;

public class IncrementalManager implements Serializable {
    private static final long serialVersionUID = 1L;
    
    protected volatile Object phase;
    protected boolean initPhase = true;
    
    @SuppressWarnings("unchecked")
    public <T> T getPhase() {
        return (T) phase;
    }
    
    public void setPhase(Object phase) {
        this.phase = phase;
    }
    
    public boolean isInitPhase() {
        return initPhase;
    }
    
    public void finishInitPhase() {
        initPhase = true;
    }
    
    public void phaseFinished() {
        
    }
    
    /**
     * Called after a solving phase has completed.
     * 
     * If this method returns true, a new phase is started immediately. Before this method returns,
     * care should be taken that all solvers that should be restarted are restarted immediately.
     * 
     * @return
     *      true if another phase should be started, false if solving is done
     */
    public boolean finishPhase() {
        return false;
    }
    
    /**
     * Called whenever the solver is about to start.
     * 
     * @param solver
     *      the solver
     */
    public void solverStart(ModuleSolver solver) {
        
    }
    
    /**
     * Called whenever the solver is done. This updates the flags of the owner of the solver.
     * 
     * @param solver
     *      the solver
     */
    public void solverDone(ModuleSolver solver) {
        solver.getOwner().setFlag(Flag.CLEAN);
    }

    @Override
    public String toString() {
        return "IncrementalManager [phase=" + phase + ", initPhase=" + initPhase + "]";
    }
}
