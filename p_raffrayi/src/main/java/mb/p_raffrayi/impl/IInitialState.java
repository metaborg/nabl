package mb.p_raffrayi.impl;

import java.util.Optional;

import mb.p_raffrayi.IUnitResult;

public interface IInitialState<S, L, D, R> {

    /**
     * True if the resource has been changed since the previous run.
     */
    boolean changed();

    /**
     * The cached result of this unit. Absent if unit is added.
     */
    Optional<IUnitResult<S, L, D, R>> previousResult();

    default boolean hasPreviousResult() {
        return previousResult().isPresent();
    }

}
