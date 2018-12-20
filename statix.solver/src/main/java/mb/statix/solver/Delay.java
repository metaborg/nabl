package mb.statix.solver;

import java.util.Set;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

/**
 * Throwable to indicate that a certain step of the solver cannot complete until more information
 * is available.
 * 
 * <p>This throwable contains the variables and/or the scopes that are required before we can
 * continue solving the constraint that threw Delay.
 */
public class Delay extends Throwable {

    private static final long serialVersionUID = 1L;

    private final Set<ITermVar> vars;
    private final Multimap<ITerm, ITerm> scopes;

    /**
     * Creates a new delay for the given variables and scope(edges).
     * 
     * @param vars
     *      a set of variables
     * @param scopes
     *      a map of scope to a set of edge labels
     */
    public Delay(Set<ITermVar> vars, Multimap<ITerm, ITerm> scopes) {
        super("delayed", null, false, false);
        this.vars = ImmutableSet.copyOf(vars);
        this.scopes = ImmutableMultimap.copyOf(scopes);
    }

    /**
     * @return
     * 		the set of variables that are required before the thrower can complete
     */
    public Set<ITermVar> vars() {
        return vars;
    }

    /**
     * @return
     * 		the map of scopes to sets of edge labels that are required before the thrower can complete
     */
    public Multimap<ITerm, ITerm> scopes() {
        return scopes;
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
        return ofVars(ImmutableSet.of(var));
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
        return new Delay(ImmutableSet.copyOf(vars), ImmutableMultimap.of());
    }

    /**
     * Builds a Delay exception for the given edge (in the form of a scope and a label).
     * 
     * @param scope
     * 		the scope that need to wait on 
     * @param label
     * 		the label of the edge that we are waiting for
     * 
     * @return
     * 		the delay
     */
    public static Delay ofScope(ITerm scope, ITerm label) {
        return new Delay(ImmutableSet.of(), ImmutableMultimap.of(scope, label));
    }

}