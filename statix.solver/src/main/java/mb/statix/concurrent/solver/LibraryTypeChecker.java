package mb.statix.concurrent.solver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.Iterables;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.Transform.T;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;

public class LibraryTypeChecker extends AbstractTypeChecker<Unit> {

    private static final ILogger logger = LoggerUtils.logger(LibraryTypeChecker.class);

    private final IStatixLibrary library;

    public LibraryTypeChecker(IStatixLibrary library, Spec spec, IDebugContext debug) {
        super(spec, debug);
        this.library = library;
    }

    @Override public IFuture<Unit> run(ITypeCheckerContext<Scope, ITerm, ITerm> context, List<Scope> rootScopes) {
        final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph = library.scopeGraph();
        final Set.Immutable<ITerm> labels = scopeGraph.getEdgeLabels();

        final Map<Scope, Scope> scopeMap = new HashMap<>();

        if(library.rootScopes().size() != rootScopes.size()) {
            throw new IllegalArgumentException("Number of root scopes does not match.");
        }
        for(int i = 0; i < rootScopes.size(); i++) {
            final Scope scope = library.rootScopes().get(i);
            if(scopeMap.containsKey(scope)) {
                continue;
            }
            final Scope newScope = rootScopes.get(i);
            scopeMap.put(scope, newScope);
            context.initScope(newScope, labels, false);
        }

        for(Scope scope : library.scopes()) {
            if(scopeMap.containsKey(scope)) {
                throw new IllegalStateException("Scope already initialized.");
            }
            final Scope newScope =
                    context.freshScope(scope.getName(), labels, scopeGraph.getData(scope).isPresent(), false);
            scopeMap.put(scope, newScope);
        }

        for(Scope scope : Iterables.concat(rootScopes, library.scopes())) {
            final Scope newScope = scopeMap.get(scope);

            final ITerm datum;
            if((datum = scopeGraph.getData(scope).orElse(null)) != null) {
                context.setDatum(newScope, subtituteScopes(datum, scopeMap));
            }

            for(ITerm label : labels) {
                for(Scope target : scopeGraph.getEdges(scope, label)) {
                    final Scope newTarget = scopeMap.get(target);
                    context.addEdge(newScope, label, newTarget);
                }
                context.closeEdge(newScope, label);
            }
        }

        return CompletableFuture.completedFuture(Unit.unit);
    }

    private ITerm subtituteScopes(ITerm datum, Map<Scope, Scope> substitution) {
        return T.sometd(Scope.matcher().map(s -> (ITerm) substitution.get(s))::match).apply(datum);
    }

}