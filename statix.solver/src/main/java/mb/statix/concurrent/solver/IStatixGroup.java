package mb.statix.concurrent.solver;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Map;
import java.util.Optional;

import org.metaborg.util.iterators.Iterables2;

import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.spec.Rule;
import mb.statix.spoofax.StatixTerms;

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

    static IMatcher<IStatixGroup> matcher() {
        return M.casesFix(m -> Iterables2.singleton(M.appl3("Group", StatixTerms.hoconstraint(),
                M.map(M.stringValue(), m), M.map(M.stringValue(), IStatixUnit.matcher()), (t, rule, groups, units) -> {
                    return new IStatixGroup() {

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
                })));
    }

}