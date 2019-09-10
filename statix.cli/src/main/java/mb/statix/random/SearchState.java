package mb.statix.random;

import java.util.Map;

import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function2;

import com.google.common.collect.ImmutableMap;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.util.CapsuleUtil;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;

public class SearchState {

    private final State state;
    private final Set.Immutable<IConstraint> constraints;
    private final ImmutableMap<ITermVar, ITermVar> existentials;

    protected SearchState(State state, Set.Immutable<IConstraint> constraints,
            ImmutableMap<ITermVar, ITermVar> existentials) {
        this.state = state;
        this.constraints = constraints;
        this.existentials = existentials;
    }

    public State state() {
        return state;
    }

    public Set.Immutable<IConstraint> constraints() {
        return constraints;
    }

    public ImmutableMap<ITermVar, ITermVar> existentials() {
        return existentials != null ? existentials : ImmutableMap.of();
    }

    public SearchState update(State state, Iterable<? extends IConstraint> constraints) {
        return new SearchState(state, CapsuleUtil.toSet(constraints), this.existentials());
    }

    public SearchState update(SolverResult result) {
        return new SearchState(result.state(), CapsuleUtil.toSet(result.delays().keySet()),
                this.existentials == null ? result.existentials() : this.existentials);
    }

    public static SearchState of(State state, Iterable<? extends IConstraint> constraints) {
        return new SearchState(state, CapsuleUtil.toSet(constraints), null);
    }

    public void print(Action1<String> printLn, Function2<ITerm, IUnifier, String> pp) {
        final IUnifier unifier = state.unifier();
        printLn.apply("SearchState");
        printLn.apply("| vars:");
        for(Map.Entry<ITermVar, ITermVar> existential : existentials.entrySet()) {
            String var = pp.apply(existential.getKey(), PersistentUnifier.Immutable.of());
            String term = pp.apply(existential.getValue(), unifier);
            printLn.apply("|   " + var + " : " + term);
        }
        printLn.apply("| constraints:");
        printLn.apply("|    " + Constraints.toString(constraints, t -> pp.apply(t, unifier)));
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        print(ln -> {
            sb.append(ln).append("\n");
        }, (t, u) -> new UnifierFormatter(u, 2).format(t));
        return sb.toString();
    }

}