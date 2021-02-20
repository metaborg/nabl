package mb.statix.concurrent.solver;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spoofax.StatixTerms;

public class InputMatchers {

    public static IMatcher<IStatixProject> project() {
        return M.appl6("Project", M.stringValue(), StatixTerms.hoconstraint(), result(),
            M.map(M.stringValue(), group()), M.map(M.stringValue(), unit()), M.map(M.stringValue(), M.req(library())),
            (t, id, rule, result, groups, units, libs) -> {
                return StatixProject.of(id, Optional.of(rule), groups, units, libs);
            });
    }

    public static IMatcher<IStatixGroup> group() {
        return M.casesFix(m -> Iterables2.singleton(M.appl5("Group", M.string(), StatixTerms.hoconstraint(), result(),
            M.map(M.stringValue(), m), M.map(M.stringValue(), unit()), (t, resource, rule, result, groups, units) -> {
                return StatixGroup.of(Optional.of(rule), groups, units);
            })));
    }

    public static IMatcher<IStatixUnit> unit() {
        return M.appl3("Unit", M.stringValue(), StatixTerms.hoconstraint(), result(), (t, resource, rule, result) -> {
            return StatixUnit.of(resource, Optional.of(rule));
        });
    }

    public static IMatcher<IStatixLibrary> library() {
        return M.appl3("Library", M.listElems(Scope.matcher()), M.listElems(Scope.matcher()), StatixTerms.scopeGraph(),
            (t, rootScopes, ownScopes, scopeGraph) -> {
                return new IStatixLibrary() {

                    @Override public List<Scope> rootScopes() {
                        return rootScopes;
                    }

                    @Override public Set<Scope> ownScopes() {
                        return ImmutableSet.copyOf(ownScopes);
                    }

                    @Override public IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph() {
                        return scopeGraph;
                    }

                };
            });
    }

    @SuppressWarnings("rawtypes") public static IMatcher<Optional<IUnitResult>> result() {
        // @formatter:off
        return M.cases(
            M.blobValue(IUnitResult.class).map(Optional::of),
            M.tuple0().map(unit -> Optional.<IUnitResult>empty())
        );
        // formatter:on
    }

}
