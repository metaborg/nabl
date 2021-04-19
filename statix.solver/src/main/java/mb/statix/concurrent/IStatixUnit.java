package mb.statix.concurrent;

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
        return M.appl2("Unit", M.stringValue(), StatixTerms.hoconstraint(), (t, resource, rule) -> {
            return StatixUnit.of(resource, Optional.of(rule));
        });
    }

}