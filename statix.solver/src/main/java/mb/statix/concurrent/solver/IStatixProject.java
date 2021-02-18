package mb.statix.concurrent.solver;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Map;
import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
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
        return M.appl6("Project", M.stringValue(), StatixTerms.hoconstraint(), resultMatcher(),
                M.map(M.stringValue(), IStatixGroup.matcher()), M.map(M.stringValue(), IStatixUnit.matcher()),
                M.map(M.stringValue(), M.req(IStatixLibrary.matcher())), (t, id, rule, result, groups, units, libs) -> {
                    return StatixProject.of(id, Optional.of(rule), groups, units, libs);
                });
    }
    
    // TODO move to proper class
    @SuppressWarnings("rawtypes")
    static IMatcher<Optional<IUnitResult>> resultMatcher() {
        return new IMatcher<Optional<IUnitResult>>() {
            @Override public Optional<Optional<IUnitResult>> match(ITerm term, IUnifier unifier) {
                return M.req("Expected either IUnitResult blob or empty tuple (for no previous result)", M.cases(
                    M.blobValue(IUnitResult.class).map(Optional::of),
                    M.tuple0().map(unit -> Optional.<IUnitResult>empty())
                )).match(term, unifier);
            }};
    }

}