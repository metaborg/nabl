package mb.statix.concurrent;

import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.Scope;
import mb.statix.spec.Rule;
import mb.p_raffrayi.impl.IInitialState;

public interface IStatixUnit {

    String resource();

    /**
     * Rule for the unit. Must have type {@literal rule : scope}.
     */
    Optional<Rule> rule();

    /**
     * Result from previous type-checker run.
     */
    IInitialState<Scope, ITerm, ITerm, UnitResult> initialState();

}