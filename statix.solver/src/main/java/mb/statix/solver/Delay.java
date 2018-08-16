package mb.statix.solver;

import java.util.Set;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

public class Delay extends Exception {

    private static final long serialVersionUID = 1L;

    private final Set<ITermVar> vars;
    private final Multimap<ITerm, ITerm> scopes;

    public Delay(Set<ITermVar> vars, Multimap<ITerm, ITerm> scopes) {
        super();
        this.vars = ImmutableSet.copyOf(vars);
        this.scopes = ImmutableMultimap.copyOf(scopes);
    }

    public Set<ITermVar> vars() {
        return vars;
    }

    public Multimap<ITerm, ITerm> scopes() {
        return scopes;
    }

    public static Delay ofVar(ITermVar var) {
        return ofVars(ImmutableSet.of(var));
    }

    public static Delay ofVars(Iterable<ITermVar> vars) {
        return new Delay(ImmutableSet.copyOf(vars), ImmutableMultimap.of());
    }

    public static Delay ofScope(ITerm scope, ITerm label) {
        return new Delay(ImmutableSet.of(), ImmutableMultimap.of(scope, label));
    }

}