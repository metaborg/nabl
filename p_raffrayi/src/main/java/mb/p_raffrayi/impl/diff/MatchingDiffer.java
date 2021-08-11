package mb.p_raffrayi.impl.diff;

import io.usethesource.capsule.Set;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.Edge;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;

public class MatchingDiffer<S, L, D> implements IScopeGraphDiffer<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(MatchingDiffer.class);

    private final IDifferOps<S, L, D> differOps;
    private final IDifferContext<S, L, D> context;
    private final BiMap.Immutable<S> scopeMatches;

    public MatchingDiffer(IDifferOps<S, L, D> differOps, IDifferContext<S, L, D> context) {
        this(differOps, context, BiMap.Immutable.of());
    }

    public MatchingDiffer(IDifferOps<S, L, D> differOps, IDifferContext<S, L, D> context, BiMap.Immutable<S> scopeMatches) {
        this.differOps = differOps;
        this.context = context;
        this.scopeMatches = scopeMatches;
    }

    @Override public IFuture<ScopeGraphDiff<S, L, D>> diff(List<S> currentRootScopes, List<S> previousRootScopes) {
        return CompletableFuture.completedFuture(ScopeGraphDiff.empty());
    }

    @Override public boolean matchScopes(BiMap.Immutable<S> scopes) {
        for(Map.Entry<S, S> entry : scopes.asMap().entrySet()) {
            final S current = entry.getKey();
            final S previous = entry.getValue();
            if(!current.equals(getCurrent(previous))) {
                return false;
            }
        }
        return true;
    }

    @Override public void typeCheckerFinished() {

    }

    @Override public IFuture<Optional<S>> match(S previousScope) {
        assertOwnScope(previousScope);
        return CompletableFuture.completedFuture(Optional.of(previousScope));
    }

    @Override public IFuture<IScopeDiff<S, L, D>> scopeDiff(S previousScope) {
        assertOwnScope(previousScope);
        final S currentScope = getCurrent(previousScope);
        return context.labels(currentScope).thenCompose(labels -> {
            final ArrayList<IFuture<Set.Immutable<Edge<S, L>>>> edgeFutures = new ArrayList<>();
            for(L label : labels) {
                edgeFutures.add(context.getEdges(currentScope, label).thenApply(tgts -> {
                    final Set.Transient<Edge<S, L>> _edges = CapsuleUtil.transientSet();
                    tgts.forEach(tgt -> _edges.__insert(new Edge<S, L>(currentScope, label, tgt)));
                    return _edges.freeze();
                }));
            }
            return AggregateFuture.of(edgeFutures).thenApply(edgeSets -> {
                final Matched.Builder<S, L, D> builder = Matched.<S, L, D>builder().currentScope(currentScope);
                edgeSets.forEach(builder::addAllMatchedEdges);
                return builder.build();
            });
        });

        // return CompletableFuture.completedFuture();
    }

    private S getCurrent(S previousScope) {
        return scopeMatches.getValueOrDefault(previousScope, previousScope);
    }

    private void assertOwnScope(S scope) {
        if(!differOps.ownScope(scope)) {
            logger.error("Scope {} not owned.", scope);
            throw new IllegalStateException("Scope " + scope + " not owned.");
        }
    }

}
