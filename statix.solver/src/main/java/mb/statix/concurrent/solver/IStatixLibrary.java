package mb.statix.concurrent.solver;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.reference.ScopeGraph;
import mb.statix.scopegraph.terms.Scope;

public interface IStatixLibrary {

    List<Scope> rootScopes();

    Set<Scope> scopes();

    IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph();

    static IMatcher<IStatixLibrary> matcher() {
        return M.appl0("Library", (t) -> {
            return new IStatixLibrary() {

                @Override public List<Scope> rootScopes() {
                    return Collections.emptyList();
                }

                @Override public Set<Scope> scopes() {
                    return Collections.emptySet();
                }

                @Override public IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph() {
                    return ScopeGraph.Immutable.of(Collections.emptySet());
                }

            };
        });
    }

}