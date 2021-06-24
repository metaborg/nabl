package mb.p_raffrayi.impl.diff;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;

public class MatchingDiffer<S, L, D> implements IScopeGraphDiffer<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(MatchingDiffer.class);

    private final IDifferOps<S, L, D> context;

    public MatchingDiffer(IDifferOps<S, L, D> context) {
        this.context = context;
    }

    @Override public IFuture<ScopeGraphDiff<S, L, D>> diff(List<S> currentRootScopes, List<S> previousRootScopes) {
        return CompletableFuture.completedFuture(ScopeGraphDiff.empty());
    }

    @Override public boolean matchScopes(BiMap.Immutable<S> scopes) {
        for(Map.Entry<S, S> entry : scopes.asMap().entrySet()) {
            final S current = entry.getKey();
            final S previous = entry.getValue();
            if(!current.equals(previous)) {
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
        return CompletableFuture.completedFuture(Matched.<S, L, D>builder().currentScope(previousScope).build());
    }

    private void assertOwnScope(S scope) {
        if(!context.ownScope(scope)) {
            logger.error("Scope {} not owned.", scope);
            throw new IllegalStateException("Scope " + scope + " not owned.");
        }
    }

}
