package mb.statix.random;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import mb.nabl2.terms.ITermVar;
import mb.statix.solver.IConstraint;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;

public class SearchState {

    private final State state;
    private final Collection<IConstraint> constraints;
    private final ImmutableMap<ITermVar, ITermVar> existentials;

    private SearchState(State state, Iterable<? extends IConstraint> constraints,
            ImmutableMap<ITermVar, ITermVar> existentials) {
        this.state = state;
        this.constraints = ImmutableList.copyOf(constraints);
        this.existentials = existentials;
    }

    public State state() {
        return state;
    }

    public Collection<IConstraint> constraints() {
        return constraints;
    }

    public ImmutableMap<ITermVar, ITermVar> existentials() {
        return existentials != null ? existentials : ImmutableMap.of();
    }

    public SearchState update(State state, Iterable<? extends IConstraint> constraints) {
        return new SearchState(state, constraints, this.existentials());
    }

    public SearchState from(SolverResult result) {
        return new SearchState(result.state(), result.delays().keySet(),
                this.existentials == null ? result.existentials() : this.existentials);
    }

    public static SearchState of(State state, Iterable<? extends IConstraint> constraints) {
        return new SearchState(state, constraints, null);
    }

}