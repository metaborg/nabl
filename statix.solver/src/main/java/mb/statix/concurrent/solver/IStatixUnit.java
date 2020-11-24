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
        return M.appl2("Unit", M.stringValue(), StatixTerms.hoconstraint(), (t, resource, rule) -> {
            return new IStatixUnit() {

                @Override public String resource() {
                    return resource;
                }

                @Override public Optional<Rule> rule() {
                    return Optional.of(rule);
                }

            };
        });
    }

}