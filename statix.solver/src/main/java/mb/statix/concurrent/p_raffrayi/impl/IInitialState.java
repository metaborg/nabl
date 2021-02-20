package mb.statix.concurrent.p_raffrayi.impl;

import java.util.Optional;

import mb.statix.concurrent.p_raffrayi.IUnitResult;

public interface IInitialState<S, L, D, R> {

    /**
     * True if the resource has been changed since the previous run.
     */
    boolean changed();

    /**
     * The cached result of this unit. Absent if unit is added.
     */
    Optional<IUnitResult<S, L, D, R>> previousResult();

    public static <S, L, D, R> IInitialState<S, L, D, R> added() {
        return InitialState.of(true, Optional.empty());
    }

    public static <S, L, D, R> IInitialState<S, L, D, R> changed(IUnitResult<S, L, D, R> previousResult) {
        return InitialState.of(true, Optional.of(previousResult));
    }

    public static <S, L, D, R> IInitialState<S, L, D, R> cached(IUnitResult<S, L, D, R> previousResult) {
        return InitialState.of(false, Optional.of(previousResult));
    }

}
