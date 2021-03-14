package mb.statix.concurrent.solver;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spoofax.StatixTerms;

public interface IStatixLibrary {

    List<Scope> rootScopes();

    Set<Scope> ownScopes();

    IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph();

    static IMatcher<IStatixLibrary> matcher() {
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

}