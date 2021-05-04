package mb.statix.concurrent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.Iterables;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.Transform.T;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;
import mb.p_raffrayi.IIncrementalTypeCheckerContext;
import mb.p_raffrayi.impl.IInitialState;
import mb.scopegraph.oopsla20.IScopeGraph;

public class LibraryTypeChecker extends AbstractTypeChecker<Unit> {

    private final IStatixLibrary library;

    public LibraryTypeChecker(IStatixLibrary library, Spec spec, IDebugContext debug) {
        super(spec, debug);
        this.library = library;
    }

    @Override public IFuture<Unit> run(IIncrementalTypeCheckerContext<Scope, ITerm, ITerm, Unit> context, List<Scope> rootScopes,
            IInitialState<Scope, ITerm, ITerm, Unit> initialState) {
        final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph = library.scopeGraph();
        final Set.Immutable<ITerm> labels = scopeGraph.getLabels();

        final Map<Scope, Scope> scopeMap = new HashMap<>();

        if(library.rootScopes().size() != rootScopes.size()) {
            throw new IllegalArgumentException("Number of root scopes does not match.");
        }
        for(int i = 0; i < rootScopes.size(); i++) {
            final Scope libRootScope = library.rootScopes().get(i);
            if(scopeMap.containsKey(libRootScope)) {
                continue;
            }
            final Scope rootScope = rootScopes.get(i);
            scopeMap.put(libRootScope, rootScope);
            context.initScope(rootScope, labels, false);
        }

        for(Scope libScope : library.ownScopes()) {
            if(scopeMap.containsKey(libScope)) {
                throw new IllegalStateException("Scope already initialized.");
            }
            final Scope scope =
                context.freshScope(libScope.getName(), labels, scopeGraph.getData(libScope).isPresent(), false);
            scopeMap.put(libScope, scope);
        }

        for(Scope libScope : Iterables.concat(library.rootScopes(), library.ownScopes())) {
            final Scope scope = scopeMap.get(libScope);

            final ITerm libDatum;
            if((libDatum = scopeGraph.getData(libScope).orElse(null)) != null) {
                final ITerm datum = subtituteScopes(libDatum, scopeMap);
                context.setDatum(scope, datum);
            }

            for(ITerm label : labels) {
                for(Scope libTarget : scopeGraph.getEdges(libScope, label)) {
                    final Scope target = scopeMap.get(libTarget);
                    context.addEdge(scope, label, target);
                }
                context.closeEdge(scope, label);
            }
        }

        return CompletableFuture.completedFuture(Unit.unit);
    }

    private ITerm subtituteScopes(ITerm datum, Map<Scope, Scope> substitution) {
        return T.sometd(Scope.matcher().map(s -> (ITerm) substitution.get(s))::match).apply(datum);
    }

}