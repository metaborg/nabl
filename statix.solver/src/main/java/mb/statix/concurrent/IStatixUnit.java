package mb.statix.concurrent;

import java.util.Optional;

import mb.statix.spec.Rule;

public interface IStatixUnit {

    String resource();

    /**
     * Rule for the unit. Must have type {@literal rule : scope}.
     */
    Optional<Rule> rule();

    /**
     * Whether this unit changed since the previous run.
     */
    boolean changed();

}