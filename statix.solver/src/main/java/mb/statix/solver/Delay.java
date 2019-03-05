package mb.statix.solver;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.CapsuleUtil;
import mb.statix.scopegraph.reference.CriticalEdge;

public class Delay extends SolverException {

    private static final long serialVersionUID = 1L;

    private final Set.Immutable<ITermVar> vars;
    private final Set.Immutable<CriticalEdge> criticalEdges;

    public Delay(Iterable<? extends ITermVar> vars, Iterable<CriticalEdge> criticalEdges) {
        super("delayed");
        this.vars = CapsuleUtil.toSet(vars);
        this.criticalEdges = CapsuleUtil.toSet(criticalEdges);
    }

    @Override public void rethrow() throws Delay, InterruptedException {
        throw this;
    }

    public Set.Immutable<ITermVar> vars() {
        return vars;
    }

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

    public static Delay ofVar(ITermVar var) {
        return ofVars(Set.Immutable.of(var));
    }

    public static Delay ofVars(Iterable<ITermVar> vars) {
        return new Delay(vars, Set.Immutable.of());
    }

    public static Delay ofCriticalEdge(CriticalEdge edge) {
        return new Delay(Set.Immutable.of(), Set.Immutable.of(edge));
    }

}