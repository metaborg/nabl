package mb.statix.library;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import static mb.nabl2.terms.matching.Transform.T;
import mb.scopegraph.library.IScopeGraphLibrary;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.IScopeGraph.Immutable;
import mb.scopegraph.oopsla20.reference.ScopeGraph;
import mb.statix.scopegraph.Scope;

public class StatixLibrary implements IStatixLibrary, IScopeGraphLibrary<Scope, ITerm, ITerm> {

    private final List<Scope> rootScopes;
    private final java.util.Set<Scope> ownScopes;
    private final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph;

    public StatixLibrary(List<Scope> rootScopes, Collection<Scope> ownScopes,
            IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph) {
        this.rootScopes = ImList.Immutable.copyOf(rootScopes);
        this.ownScopes = CapsuleUtil.toSet(ownScopes);
        this.scopeGraph = scopeGraph;
    }

    @Override public List<Scope> rootScopes() {
        return rootScopes;
    }

    @Override public java.util.Set<Scope> ownScopes() {
        return ownScopes;
    }

    @Override public IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph() {
        return scopeGraph;
    }

    @Override public Tuple2<Set.Immutable<Scope>, Immutable<Scope, ITerm, ITerm>> initialize(List<Scope> rootScopes,
            Function1<String, Scope> freshScope) {
        final Set<ITerm> edgeLabels = this.scopeGraph.getLabels();

        final Map<Scope, Scope> scopeMap = new HashMap<>();
        final io.usethesource.capsule.Set.Transient<Scope> ownScopes = CapsuleUtil.transientSet();
        final IScopeGraph.Transient<Scope, ITerm, ITerm> scopeGraph = ScopeGraph.Transient.of();

        // map library scopes to actual scopes

        if(this.rootScopes.size() != rootScopes.size()) {
            throw new IllegalArgumentException("Number of root scopes does not match.");
        }
        for(int i = 0; i < rootScopes.size(); i++) {
            final Scope libRootScope = this.rootScopes.get(i);
            if(scopeMap.containsKey(libRootScope)) {
                continue;
            }
            final Scope rootScope = rootScopes.get(i);
            scopeMap.put(libRootScope, rootScope);
        }
        for(Scope libScope : this.ownScopes) {
            if(scopeMap.containsKey(libScope)) {
                throw new IllegalStateException("Scope already initialized.");
            }
            final Scope scope = freshScope.apply(libScope.getName());
            ownScopes.__insert(scope);
            scopeMap.put(libScope, scope);
        }

        // add data and edges to actual scopes

        for(Scope libScope : this.rootScopes) {
            final Scope scope = scopeMap.get(libScope);

            for(ITerm label : edgeLabels) {
                for(Scope libTarget : this.scopeGraph.getEdges(libScope, label)) {
                    final Scope target = scopeMap.get(libTarget);
                    scopeGraph.addEdge(scope, label, target);
                }
            }
        }
        for(Scope libScope : this.ownScopes) {
            final Scope scope = scopeMap.get(libScope);

            final ITerm libDatum;
            if((libDatum = this.scopeGraph.getData(libScope).orElse(null)) != null) {
                final ITerm datum = substituteScopes(libDatum, scopeMap);
                scopeGraph.setDatum(scope, datum);
            }

            for(ITerm label : edgeLabels) {
                for(Scope libTarget : this.scopeGraph.getEdges(libScope, label)) {
                    final Scope target = scopeMap.get(libTarget);
                    scopeGraph.addEdge(scope, label, target);
                }
            }
        }

        return Tuple2.of(ownScopes.freeze(), scopeGraph.freeze());
    }

    private static ITerm substituteScopes(ITerm datum, Map<Scope, Scope> substitution) {
        return T.sometd(Scope.matcher().map(s -> (ITerm) substitution.getOrDefault(s, s))::match).apply(datum);
    }

}
