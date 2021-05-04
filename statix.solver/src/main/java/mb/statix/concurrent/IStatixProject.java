package mb.statix.concurrent;

import java.util.Map;
import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.Scope;
import mb.statix.spec.Rule;
import mb.p_raffrayi.impl.IInitialState;

public interface IStatixProject {

    String resource();

    /**
     * Rule for the project. Must have type {@literal rule : scope}.
     */
    Optional<Rule> rule();

    /**
     * Direct sub groups of this project.
     */
    Map<String, IStatixGroup> groups();

    /**
     * Direct sub units of this project.
     */
    Map<String, IStatixUnit> units();

    /**
     * Direct sub units of this project.
     */
    Map<String, IStatixLibrary> libraries();

    /**
     * Result from previous type-checker run.
     */
    IInitialState<Scope, ITerm, ITerm, ProjectResult> initialState();

}