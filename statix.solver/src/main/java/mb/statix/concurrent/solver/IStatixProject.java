package mb.statix.concurrent.solver;

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

    static IMatcher<IStatixProject> matcher() {
        return M.appl4("Project", M.stringValue(), StatixTerms.hoconstraint(),
                M.map(M.stringValue(), IStatixGroup.matcher()), M.map(M.stringValue(), IStatixUnit.matcher()),
                (t, id, rule, groups, units) -> {
                    return new IStatixProject() {

                        @Override public String resource() {
                            return id;
                        }

                        @Override public Optional<Rule> rule() {
                            return Optional.of(rule);
                        }

                        @Override public Map<String, IStatixGroup> groups() {
                            return groups;
                        }

                        @Override public Map<String, IStatixUnit> units() {
                            return units;
                        }

                    };
                });
    }

}