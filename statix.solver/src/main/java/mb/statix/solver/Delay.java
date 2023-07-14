package mb.statix.solver;

import java.util.Collection;

import org.metaborg.util.collection.CapsuleUtil;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

public class Delay extends Throwable {

    private static final long serialVersionUID = 1L;

    private final Set.Immutable<ITermVar> vars;
    private final Set.Immutable<CriticalEdge> criticalEdges;

    public Delay(Iterable<? extends ITermVar> vars, Iterable<CriticalEdge> criticalEdges) {
        super("delayed", null, false, false);
        this.vars = CapsuleUtil.toSet(vars);
        this.criticalEdges = CapsuleUtil.toSet(criticalEdges);
    }

    @Override public String getMessage() {
        final StringBuilder sb = new StringBuilder();
        sb.append("delayed on ");
        sb.append("vars ").append(vars);
        sb.append(" and ");
        sb.append("criticalEdges ").append(criticalEdges);
        return sb.toString();
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
        final Set.Transient<CriticalEdge> retainedCriticalEdges = CapsuleUtil.transientSet();
        this.criticalEdges.stream().filter(ce -> scopeSet.contains(ce.scope()))
                .forEach(retainedCriticalEdges::__insert);
        return new Delay(retainedVars, retainedCriticalEdges.freeze());
    }

    public Delay removeAll(Iterable<? extends ITermVar> vars, Iterable<? extends ITerm> scopes) {
        final Set.Immutable<ITermVar> retainedVars = this.vars.__removeAll(CapsuleUtil.toSet(vars));
        final Set.Immutable<ITerm> scopeSet = CapsuleUtil.toSet(scopes);
        final Set.Transient<CriticalEdge> retainedCriticalEdges = CapsuleUtil.transientSet();
        this.criticalEdges.stream().filter(ce -> !scopeSet.contains(ce.scope()))
                .forEach(retainedCriticalEdges::__insert);
        return new Delay(retainedVars, retainedCriticalEdges.freeze());
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Delay(");
        sb.append("vars=").append(vars);
        sb.append(", ");
        sb.append("criticalEdges=").append(criticalEdges);
        sb.append(")");
        return sb.toString();
    }

    public static Delay ofVar(ITermVar var) {
        return ofVars(CapsuleUtil.immutableSet(var));
    }

    public static Delay ofVars(Iterable<ITermVar> vars) {
        return new Delay(vars, CapsuleUtil.immutableSet());
    }

    public static Delay ofCriticalEdge(CriticalEdge edge) {
        return new Delay(CapsuleUtil.immutableSet(), CapsuleUtil.immutableSet(edge));
    }

    public static Delay of(Collection<Delay> delays) {
        Set.Transient<ITermVar> vars = CapsuleUtil.transientSet();
        Set.Transient<CriticalEdge> scopes = CapsuleUtil.transientSet();
        delays.stream().forEach(d -> {
            vars.__insertAll(d.vars());
            scopes.__insertAll(d.criticalEdges());
        });
        return new Delay(vars.freeze(), scopes.freeze());
    }

}