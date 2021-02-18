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
        return M.casesFix(m -> Iterables2.singleton(M.appl5("Group", M.string(), StatixTerms.hoconstraint(), IStatixProject.resultMatcher(),
                M.map(M.stringValue(), m), M.map(M.stringValue(), IStatixUnit.matcher()), (t, resource, rule, result, groups, units) -> {
                    return StatixGroup.of(Optional.of(rule), groups, units);
                })));
    }

}