package mb.p_raffrayi.impl.diff;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.tuple.Tuple2;

import com.google.common.collect.Multimap;

import mb.scopegraph.oopsla20.IScopeGraph.Immutable;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.Edge;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.patching.IPatchCollection;

public class MatchingDiffer<S, L, D> implements IScopeGraphDiffer<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(MatchingDiffer.class);

    private final IDifferOps<S, L, D> differOps;
    private final IDifferContext<S, L, D> context;
    private final BiMap.Immutable<S> scopeMatches;

    public MatchingDiffer(IDifferOps<S, L, D> differOps, IDifferContext<S, L, D> context) {
        this(differOps, context, BiMap.Immutable.of());
    }

    public MatchingDiffer(IDifferOps<S, L, D> differOps, IDifferContext<S, L, D> context,
            BiMap.Immutable<S> scopeMatches) {
        this.differOps = differOps;
        this.context = context;
        this.scopeMatches = scopeMatches;
    }

    public MatchingDiffer(IDifferOps<S, L, D> differOps, IDifferContext<S, L, D> context,
            Iterable<Map.Entry<S, S>> scopeMatches) {
        this.differOps = differOps;
        this.context = context;

        final BiMap.Transient<S> _scopeMatches = BiMap.Transient.of();
        for(Map.Entry<S, S> match : scopeMatches) {
            _scopeMatches.put(match.getKey(), match.getValue());
        }
        this.scopeMatches = _scopeMatches.freeze();
    }

    @Override public IFuture<ScopeGraphDiff<S, L, D>> diff(List<S> currentRootScopes, List<S> previousRootScopes) {
        // TODO: construct proper diff
        return CompletableFuture.completedFuture(ScopeGraphDiff.empty());
    }

    @Override public IFuture<ScopeGraphDiff<S, L, D>> diff(Immutable<S, L, D> initiallyMatchedGraph,
            Collection<S> scopes, Collection<S> sharedScopes, IPatchCollection.Immutable<S> patches,
            Collection<S> openScopes, Multimap<S, EdgeOrData<L>> openEdges) {
        if(!openScopes.isEmpty() || !openEdges.isEmpty() || !patches.isIdentity()) {
            throw new IllegalStateException("Cannot create matching differ with open scopes/edges.");
        }
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

    private final Map<Tuple2<S, L>, IFuture<ScopeDiff<S, L, D>>> scopeDiffs = new HashMap<>();

    @Override public IFuture<ScopeDiff<S, L, D>> scopeDiff(S previousScope, L label) {
        assertOwnScope(previousScope);
        return scopeDiffs.computeIfAbsent(Tuple2.of(previousScope, label), __ -> {
            final S currentScope = getCurrent(previousScope);
            return context.getEdges(currentScope, label).thenApply(tgts -> {
                final ScopeDiff.Builder<S, L, D> builder = ScopeDiff.builder();
                tgts.forEach(tgt -> builder.addMatchedEdges(new Edge<S, L>(currentScope, label, tgt)));
                return builder.build();
            });
        });
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
