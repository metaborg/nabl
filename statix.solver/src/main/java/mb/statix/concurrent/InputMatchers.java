package mb.statix.concurrent;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;

import org.metaborg.util.iterators.Iterables2;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.scopegraph.Scope;
import mb.statix.spoofax.StatixTerms;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.impl.AInitialState;
import mb.p_raffrayi.impl.IInitialState;

public class InputMatchers {

    public static IMatcher<IStatixProject> project() {
        return M.appl6("Project", M.stringValue(), StatixTerms.hoconstraint(), InputMatchers.<ProjectResult>initialState(),
            M.map(M.stringValue(), group()), M.map(M.stringValue(), unit()), M.map(M.stringValue(), M.req(library())),
            (t, id, rule, result, groups, units, libs) -> {
                return StatixProject.of(id, Optional.of(rule), groups, units, libs, result);
            });
    }

    public static IMatcher<IStatixGroup> group() {
        return M.casesFix(m -> Iterables2.singleton(
            M.appl5("Group", M.string(), StatixTerms.hoconstraint(), InputMatchers.<GroupResult>initialState(), M.map(M.stringValue(), m),
                M.map(M.stringValue(), unit()), (t, resource, rule, result, groups, units) -> {
                    return StatixGroup.of(Optional.of(rule), groups, units, result);
                })));
    }

    public static IMatcher<IStatixUnit> unit() {
        return M.appl3("Unit", M.stringValue(), StatixTerms.hoconstraint(), InputMatchers.<UnitResult>initialState(),
            (t, resource, rule, result) -> {
                return StatixUnit.of(resource, Optional.of(rule), result);
            });
    }

    public static IMatcher<IStatixLibrary> library() {
        return M.appl3("Library", M.listElems(Scope.matcher()), M.listElems(Scope.matcher()), StatixTerms.scopeGraph(),
            (t, rootScopes, ownScopes, scopeGraph) -> {
                return new StatixLibrary(rootScopes, ownScopes, scopeGraph);
            });
    }

    @SuppressWarnings("unchecked") public static <R> IMatcher<IInitialState<Scope, ITerm, ITerm, R>> initialState() {
        // @formatter:off
        return M.cases(
            M.appl0("Added", appl -> AInitialState.added()),
            M.appl1("Cached", M.blobValue(IUnitResult.class), (appl, result) -> AInitialState.cached(result)),
            M.appl1("Changed", M.blobValue(IUnitResult.class), (appl, result) -> AInitialState.changed(result))
        );
        // formatter:on
    }

}