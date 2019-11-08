package mb.statix.generator;

import java.util.Map.Entry;

import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.util.CapsuleUtil;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.persistent.SolverResult;

public class SearchState {

    private final static ILogger log = LoggerUtils.logger(SearchState.class);

    protected final IState.Immutable state;
    protected final Set.Immutable<IConstraint> constraints;
    protected final Map.Immutable<IConstraint, Delay> delays;
    protected final ImmutableMap<ITermVar, ITermVar> existentials;
    protected final ICompleteness.Immutable completeness;

    protected SearchState(IState.Immutable state, Set.Immutable<IConstraint> constraints,
            Map.Immutable<IConstraint, Delay> delays, ImmutableMap<ITermVar, ITermVar> existentials,
            ICompleteness.Immutable completeness) {
        this.state = state;
        this.constraints = constraints;
        this.delays = delays;
        this.existentials = existentials;
        this.completeness = completeness;
    }

    public IState.Immutable state() {
        return state;
    }

    public Set.Immutable<IConstraint> constraints() {
        return constraints;
    }

    public Map.Immutable<IConstraint, Delay> delays() {
        return delays;
    }

    public Iterable<IConstraint> constraintsAndDelays() {
        return Iterables.concat(constraints, delays.keySet());
    }

    public ImmutableMap<ITermVar, ITermVar> existentials() {
        return existentials != null ? existentials : ImmutableMap.of();
    }

    public ICompleteness.Immutable completeness() {
        return completeness;
    }

    /**
     * Update the constraints in this set, keeping completeness and delayed constraints in sync.
     * 
     * This method assumes that no constraints appear in both add and remove, or it will be incorrect!
     */
    public SearchState update(Iterable<IConstraint> add, Iterable<IConstraint> remove) {
        final ICompleteness.Transient completeness = this.completeness.melt();
        final Set.Transient<IConstraint> constraints = this.constraints.asTransient();
        final java.util.Set<CriticalEdge> removedEdges = Sets.newHashSet();
        add.forEach(c -> {
            if(constraints.__insert(c)) {
                completeness.add(c, state.unifier());
            }
        });
        remove.forEach(c -> {
            if(constraints.__remove(c)) {
                removedEdges.addAll(completeness.remove(c, state.unifier()));
            }
        });
        final Map.Transient<IConstraint, Delay> delays = Map.Transient.of();
        this.delays.forEach((c, d) -> {
            if(!Sets.intersection(d.criticalEdges(), removedEdges).isEmpty()) {
                constraints.__insert(c);
            } else {
                delays.__put(c, d);
            }
        });
        return new SearchState(state, constraints.freeze(), delays.freeze(), existentials, completeness.freeze());
    }

    public SearchState delay(Iterable<? extends Entry<IConstraint, Delay>> delay) {
        final Set.Transient<IConstraint> constraints = this.constraints.asTransient();
        final Map.Transient<IConstraint, Delay> delays = this.delays.asTransient();
        delay.forEach(entry -> {
            if(constraints.__remove(entry.getKey())) {
                delays.__put(entry.getKey(), entry.getValue());
            } else {
                log.warn("delayed constraint not in constraint set: {}", entry.getKey());
            }
        });
        return new SearchState(state, constraints.freeze(), delays.freeze(), existentials, completeness);
    }

    /**
     * Replace the current state and constraints by the result from solving.
     */
    public SearchState replace(SolverResult result) {
        final Set.Transient<IConstraint> constraints = Set.Transient.of();
        final Map.Transient<IConstraint, Delay> delays = Map.Transient.of();
        result.delays().forEach((c, d) -> {
            if(d.criticalEdges().isEmpty()) {
                constraints.__insert(c);
            } else {
                delays.__put(c, d);
            }
        });
        final ImmutableMap<ITermVar, ITermVar> existentials =
                this.existentials == null ? result.existentials() : this.existentials;
        return new SearchState(result.state(), constraints.freeze(), delays.freeze(), existentials,
                result.completeness());
    }

    public SearchState replace(IState.Immutable state, Set.Immutable<IConstraint> constraints,
            Map.Immutable<IConstraint, Delay> delays, ICompleteness.Immutable completeness) {
        return new SearchState(state, constraints, delays, existentials, completeness);
    }

    public static SearchState of(IState.Immutable state, Iterable<? extends IConstraint> constraints) {
        final ICompleteness.Transient completeness = Completeness.Transient.of(state.spec());
        completeness.addAll(constraints, state.unifier());
        return new SearchState(state, CapsuleUtil.toSet(constraints), Map.Immutable.of(), null, completeness.freeze());
    }

    public void print(Action1<String> printLn, Function2<ITerm, IUnifier, String> pp) {
        final IUnifier unifier = state.unifier();
        printLn.apply("SearchState");
        printLn.apply("| vars:");
        for(Map.Entry<ITermVar, ITermVar> existential : existentials().entrySet()) {
            String var = pp.apply(existential.getKey(), Unifiers.Immutable.of());
            String term = pp.apply(existential.getValue(), unifier);
            printLn.apply("|   " + var + " : " + term);
        }
        printLn.apply("| unifier: " + state.unifier().toString());
        printLn.apply("| completeness: " + completeness.toString());
        printLn.apply("| constraints:");
        for(IConstraint c : constraints) {
            printLn.apply("|   " + c.toString(t -> pp.apply(t, unifier)));
        }
        printLn.apply("| delays:");
        for(Entry<IConstraint, Delay> e : delays.entrySet()) {
            printLn.apply("|   " + e.getValue() + " : " + e.getKey().toString(t -> pp.apply(t, unifier)));
        }
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        print(ln -> {
            sb.append(ln).append("\n");
        }, (t, u) -> new UnifierFormatter(u, 2).format(t));
        return sb.toString();
    }

}