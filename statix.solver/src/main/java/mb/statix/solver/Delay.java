package mb.statix.solver;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.CapsuleUtil;
import mb.statix.scopegraph.reference.CriticalEdge;

/**
 * Throwable to indicate that a certain step of the solver cannot complete until more information
 * is available.
 * 
 * <p>This throwable contains the variables and/or the scopes that are required before we can
 * continue solving the constraint that threw Delay.
 */
public class Delay extends Throwable {

    private static final long serialVersionUID = 1L;

    private final Set.Immutable<ITermVar> vars;
    private final Set.Immutable<CriticalEdge> criticalEdges;

    /**
     * Creates a new delay for the given variables and critical edges.
     * 
     * @param vars
     *      an iterable of variables
     * @param criticalEdges
     *      an iterable of critical edges
     */
    public Delay(Iterable<? extends ITermVar> vars, Iterable<CriticalEdge> criticalEdges) {
        super("delayed", null, false, false);
        this.vars = CapsuleUtil.toSet(vars);
        this.criticalEdges = CapsuleUtil.toSet(criticalEdges);
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

    public Delay retainAll(Iterable<? extends ITermVar> vars, Iterable<? extends ITerm> scopes) {
        final Set.Immutable<ITermVar> retainedVars = this.vars.__retainAll(CapsuleUtil.toSet(vars));
        final Set.Immutable<ITerm> scopeSet = CapsuleUtil.toSet(scopes);
        final Set.Transient<CriticalEdge> retainedCriticalEdges = Set.Transient.of();
        this.criticalEdges.stream().filter(ce -> scopeSet.contains(ce.scope()))
                .forEach(retainedCriticalEdges::__insert);
        return new Delay(retainedVars, retainedCriticalEdges.freeze());
    }

    public static Delay of() {
        return new Delay(Set.Immutable.of(), Set.Immutable.of());
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
        return new Delay(vars, Set.Immutable.of());
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
        return new Delay(Set.Immutable.of(), Set.Immutable.of(edge));
    }

}