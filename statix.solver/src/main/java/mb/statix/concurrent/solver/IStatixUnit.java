package mb.statix.concurrent.solver;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;

import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.spec.Rule;
import mb.statix.spoofax.StatixTerms;

public interface IStatixUnit {

    String resource();

    /**
     * Rule for the unit. Must have type {@literal rule : scope}.
     */
    Optional<Rule> rule();

    static IMatcher<IStatixUnit> matcher() {
        return M.appl3("Unit", M.stringValue(), StatixTerms.hoconstraint(), IStatixProject.resultMatcher(), (t, resource, rule, result) -> {
            return StatixUnit.of(resource, Optional.of(rule));
        });
    }

}