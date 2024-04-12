package mb.statix.concurrent;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;

import org.metaborg.util.iterators.Iterables2;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.library.IStatixLibrary;
import mb.statix.library.StatixLibrary;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.tracer.SolverTracer;
import mb.statix.spoofax.StatixTerms;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.impl.Result;

public class InputMatchers {

    public static <TR extends SolverTracer.IResult<TR>> IMatcher<IStatixProject<TR>> project() {
        return M.appl6("Project", M.stringValue(), StatixTerms.hoconstraint(), InputMatchers.<TR>previousResult(),
                M.map(M.stringValue(), group()), M.map(M.stringValue(), unit()),
                M.map(M.stringValue(), M.req(library())), (t, id, rule, result, groups, units, libs) -> {
                    return StatixProject.of(id, Optional.of(rule), groups, units, libs, result.isPresent(),
                            result.orElse(null));
                });
    }

    public static IMatcher<IStatixGroup> group() {
        return M.req("Expected Group",
                M.casesFix(m -> Iterables2.singleton(M.appl6("Group", M.stringValue(), M.listElems(M.stringValue()),
                        StatixTerms.hoconstraint(), changed(), M.map(M.stringValue(), m),
                        M.map(M.stringValue(), unit()), (t, resource, scopeNames, rule, changed, groups, units) -> {
                            return StatixGroup.of(resource, scopeNames, Optional.of(rule), changed, groups, units);
                        }))));
    }

    public static IMatcher<IStatixUnit> unit() {
        return M.req("Expected Unit", M.appl3("Unit", M.stringValue(), StatixTerms.hoconstraint(),
                InputMatchers.changed(), (t, resource, rule, result) -> {
                    return StatixUnit.of(resource, Optional.of(rule), result);
                }));
    }

    public static IMatcher<IStatixLibrary> library() {
        return M.req("Expected Library", M.appl3("Library", M.listElems(Scope.matcher()), M.listElems(Scope.matcher()),
                StatixTerms.scopeGraph(), (t, rootScopes, ownScopes, scopeGraph) -> {
                    return new StatixLibrary(rootScopes, ownScopes, scopeGraph);
                }));
    }

    public static IMatcher<Boolean> changed() {
        // @formatter:off
        return M.req("Expected Change indicator", M.cases(
            M.appl0("Cached", appl -> false),
            M.appl0("Changed", appl -> true)
        ));
        // formatter:on
    }

    @SuppressWarnings("unchecked")
    public static <TR extends SolverTracer.IResult<TR>> IMatcher<Optional<IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, ProjectResult<TR>, SolverState>>>> previousResult() {
        // @formatter:off
        return M.req("Expected Unit Result option.", M.<Optional<IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, ProjectResult<TR>, SolverState>>>>cases(
            M.appl0("Added", appl -> Optional.empty()),
            M.appl1("Cached", M.blobValue(IUnitResult.class), (appl, result) -> Optional.<IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, ProjectResult<TR>, SolverState>>>of(result))
        ));
        // formatter:on
    }

}
