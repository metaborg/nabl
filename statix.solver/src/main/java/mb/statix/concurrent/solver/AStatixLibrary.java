package mb.statix.concurrent.solver;

import java.util.List;
import java.util.Set;

import org.immutables.value.Value;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.terms.Scope;

@Value.Immutable
public abstract class AStatixLibrary implements IStatixLibrary {

    @Value.Parameter @Override public abstract List<Scope> rootScopes();

    @Value.Parameter @Override public abstract Set<Scope> ownScopes();

    @Value.Parameter @Override public abstract IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph();

}