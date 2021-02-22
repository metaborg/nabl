package mb.statix.concurrent.solver;

import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.p_raffrayi.impl.IInitialState;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spec.Rule;

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