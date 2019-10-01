package mb.statix.random;

import org.metaborg.util.functions.Function1;

public class EitherSearchState<O1 extends SearchState, O2 extends SearchState> extends SearchState {

    private final O1 left;
    private final O2 right;

    private EitherSearchState(SearchState state, O1 left, O2 right) {
        super(state.state, state.constraints, state.delays, state.existentials, state.completeness);
        this.left = left;
        this.right = right;
    }

    public <R> R map(Function1<O1, R> mapLeft, Function1<O2, R> mapRight) {
        if(left != null) {
            return mapLeft.apply(left);
        } else {
            return mapRight.apply(right);
        }
    }

    public static <O1 extends SearchState, O2 extends SearchState> EitherSearchState<O1, O2> ofLeft(O1 left) {
        return new EitherSearchState<>(left, left, null);
    }

    public static <O1 extends SearchState, O2 extends SearchState> EitherSearchState<O1, O2> ofRight(O2 right) {
        return new EitherSearchState<>(right, null, right);
    }

}