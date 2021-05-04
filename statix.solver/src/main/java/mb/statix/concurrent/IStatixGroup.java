package mb.statix.concurrent;

import java.util.Map;
import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.Scope;
import mb.statix.spec.Rule;
import mb.p_raffrayi.impl.IInitialState;

public interface IStatixGroup {

    /**
     * Rule for the group. Must have type {@literal rule : scope * scope}.
     */
    Optional<Rule> rule();

    /**
     * Direct sub groups of this group.
     */
    Map<String, IStatixGroup> groups();

    /**
     * Direct sub units of this group.
     */
    Map<String, IStatixUnit> units();

    /**
     * Result of previous type-checker run.
     */
    IInitialState<Scope, ITerm, ITerm, GroupResult> initialState();

}