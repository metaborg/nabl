package mb.statix.concurrent;

import java.util.Map;
import java.util.Optional;

import mb.statix.spec.Rule;

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

}