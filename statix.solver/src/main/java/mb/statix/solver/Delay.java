package mb.statix.solver;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.CapsuleUtil;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.taico.scopegraph.locking.LockManager;

/**
 * Throwable to indicate that a certain step of the solver cannot complete until more information
 * is available.
 * 
 * <p>This throwable contains the variables and/or the scopes that are required before we can
 * continue solving the constraint that threw Delay.
 */
public class Delay extends SolverException {

    private static final long serialVersionUID = 1L;

    private final Set.Immutable<ITermVar> vars;
    private final Set.Immutable<CriticalEdge> criticalEdges;
    private transient LockManager lockManager;

    public Delay(Iterable<? extends ITermVar> vars, Iterable<CriticalEdge> criticalEdges) {
        this(vars, criticalEdges, null);
    }
    
    /**
     * Creates a new delay for the given variables and critical edges.
     * 
     * @param vars
     *      an iterable of variables
     * @param criticalEdges
     *      an iterable of critical edges
     * @param lockManager
     *      the lock manager
     */
    public Delay(Iterable<? extends ITermVar> vars, Iterable<CriticalEdge> criticalEdges, LockManager lockManager) {
        super("delayed");
        this.vars = CapsuleUtil.toSet(vars);
        this.criticalEdges = CapsuleUtil.toSet(criticalEdges);
        this.lockManager = lockManager;
    }

    @Override public void rethrow() throws Delay, InterruptedException {
        throw this;
    }

    /**
     * @return
     *      the set of variables that are required before the thrower can complete
     */
    public Set.Immutable<ITermVar> vars() {
        return vars;
    }

    /**
     * @return
     *      the set of critical edges that are required before the thrower can complete
     */
    public Set.Immutable<CriticalEdge> criticalEdges() {
        return criticalEdges;
    }
    
    /**
     * @return
     *      the lock manager, or null if none was set
     */
    public LockManager getLockManager() {
        return lockManager;
    }
    
    /**
     * @param lockManager
     * 
     * @throws IllegalStateException
     *      If the old lock manager still holds locks.
     */
    public void setLockManager(LockManager lockManager) {
        if (this.lockManager != null && !this.lockManager.isDone()) {
            throw new IllegalStateException("Own lock manager (" + this.lockManager + ") is not empty, but is being replaced with " + lockManager + "!");
        }
        this.lockManager = lockManager;
    }

    public Delay retainAll(Iterable<? extends ITermVar> vars, Iterable<? extends ITerm> scopes) {
        final Set.Immutable<ITermVar> retainedVars = this.vars.__retainAll(CapsuleUtil.toSet(vars));
        final Set.Immutable<ITerm> scopeSet = CapsuleUtil.toSet(scopes);
        final Set.Transient<CriticalEdge> retainedCriticalEdges = Set.Transient.of();
        this.criticalEdges.stream().filter(ce -> scopeSet.contains(ce.scope()))
                .forEach(retainedCriticalEdges::__insert);
        return new Delay(retainedVars, retainedCriticalEdges.freeze(), this.lockManager);
    }

    public Delay removeAll(Iterable<? extends ITermVar> vars, Iterable<? extends ITerm> scopes) {
        final Set.Immutable<ITermVar> retainedVars = this.vars.__removeAll(CapsuleUtil.toSet(vars));
        final Set.Immutable<ITerm> scopeSet = CapsuleUtil.toSet(scopes);
        final Set.Transient<CriticalEdge> retainedCriticalEdges = Set.Transient.of();
        this.criticalEdges.stream().filter(ce -> !scopeSet.contains(ce.scope()))
                .forEach(retainedCriticalEdges::__insert);
        return new Delay(retainedVars, retainedCriticalEdges.freeze());
    }

    public static Delay of() {
        return new Delay(Set.Immutable.of(), Set.Immutable.of(), null);
    }

    /**
     * Builds a Delay exception for the given variable.
     * 
     * @param var
     *      the variable that we are waiting for
     * 
     * @return
     *      the delay
     */
    public static Delay ofVar(ITermVar var) {
        return ofVars(Set.Immutable.of(var));
    }

    /**
     * Builds a Delay exception for the given variables.
     * 
     * @param vars
     *      the variables that we are waiting for
     * 
     * @return
     * 		the delay
     */
    public static Delay ofVars(Iterable<ITermVar> vars) {
        return new Delay(vars, Set.Immutable.of(), null);
    }

    /**
     * Builds a Delay exception for the given critical edge.
     * 
     * @param edge
     *      the critical edge that we are waiting for
     * 
     * @return
     *      the delay
     */
    public static Delay ofCriticalEdge(CriticalEdge edge) {
        return new Delay(Set.Immutable.of(), Set.Immutable.of(edge), null);
    }
    
    /**
     * Builds a Delay exception for the given critical edge.
     * 
     * @param edge
     *      the critical edge that we are waiting for
     * @param lockManager
     *      the lock manager to release locks from
     * 
     * @return
     *      the delay
     */
    public static Delay ofCriticalEdge(CriticalEdge edge, LockManager lockManager) {
        return new Delay(Set.Immutable.of(), Set.Immutable.of(edge), lockManager);
    }

}