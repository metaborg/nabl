package mb.statix.concurrent.solver;

import java.util.List;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.terms.Scope;

public interface IStatixLibrary {

    List<Scope> rootScopes();

    Set<Scope> ownScopes();

    IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph();

}