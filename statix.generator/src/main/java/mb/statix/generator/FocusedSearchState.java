package mb.statix.generator;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;

public class FocusedSearchState<C extends IConstraint> extends SearchState {

    private final C focus;
    private final Set.Immutable<IConstraint> unfocused;

    private FocusedSearchState(IState.Immutable state, Set.Immutable<IConstraint> constraints, C focus,
            Map.Immutable<IConstraint, Delay> delays, Map.Immutable<ITermVar, ITermVar> existentials,
            ICompleteness.Immutable completeness) {
        super(state, constraints, delays, existentials, completeness);
        if(!constraints.contains(focus)) {
            throw new IllegalArgumentException("Must focus on a constraint in the set");
        }
        this.focus = focus;
        this.unfocused = constraints.__remove(focus);
    }

    public Set.Immutable<IConstraint> unfocused() {
        return unfocused;
    }

    public C focus() {
        return focus;
    }

    public static <C extends IConstraint> FocusedSearchState<C> of(SearchState state, C focus) {
        return new FocusedSearchState<>(state.state(), state.constraints(), focus, state.delays(), state.existentials(),
                state.completeness());
    }

}