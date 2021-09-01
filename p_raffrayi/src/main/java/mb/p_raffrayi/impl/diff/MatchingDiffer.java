package mb.p_raffrayi.impl.diff;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @Override public IFuture<ScopeDiff<S, L, D>> scopeDiff(S previousScope, L label) {
        assertOwnScope(previousScope);
        final S currentScope = getCurrent(previousScope);
        return context.getEdges(currentScope, label).thenApply(tgts -> {
            final ScopeDiff.Builder<S, L, D> builder = ScopeDiff.builder();
            tgts.forEach(tgt -> builder.addMatchedEdges(new Edge<S, L>(currentScope, label, tgt)));
            return builder.build();
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
