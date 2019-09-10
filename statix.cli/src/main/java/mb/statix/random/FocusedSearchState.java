package mb.statix.random;

import com.google.common.collect.ImmutableMap;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;
import mb.statix.solver.IConstraint;
import mb.statix.solver.persistent.State;

public class FocusedSearchState<C extends IConstraint> extends SearchState {

    private final C focus;

    protected FocusedSearchState(State state, Set.Immutable<IConstraint> constraints, C focus,
            ImmutableMap<ITermVar, ITermVar> existentials) {
        super(state, constraints.__remove(focus), existentials);
        this.focus = focus;
    }

    public C focus() {
        return focus;
    }

    public static <C extends IConstraint> FocusedSearchState<C> of(SearchState state, C focus) {
        return new FocusedSearchState<>(state.state(), state.constraints(), focus, state.existentials());
    }

}