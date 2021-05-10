package mb.statix.concurrent;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Map;
import java.util.Optional;

import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.spec.Rule;
import mb.statix.spoofax.StatixTerms;

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

    static IMatcher<IStatixProject> matcher() {
        return M.appl5("Project", M.stringValue(), StatixTerms.hoconstraint(),
                M.map(M.stringValue(), IStatixGroup.matcher()), M.map(M.stringValue(), IStatixUnit.matcher()),
                M.map(M.stringValue(), M.req(IStatixLibrary.matcher())), (t, id, rule, groups, units, libs) -> {
                    return StatixProject.of(id, Optional.of(rule), groups, units, libs);
                });
    }

}