package mb.p_raffrayi.impl.confirm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.Futures;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;

import com.google.common.collect.Sets;

import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.impl.IQueryAnswer;
import mb.p_raffrayi.impl.Release;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public class TrivialConfirmation<S, L, D> implements IConfirmation<S, L, D> {

    private final IConfirmationContext<S, L, D> context;

    public TrivialConfirmation(IConfirmationContext<S, L, D> context) {
        this.context = context;
    }

    @Override public IFuture<Optional<BiMap.Immutable<S>>> confirm(java.util.Set<IRecordedQuery<S, L, D>> queries) {
        final ICompletableFuture<Optional<BiMap.Immutable<S>>> result = new CompletableFuture<>();

        final List<IFuture<Boolean>> futures = new ArrayList<>();
        queries.forEach(rq -> {
            final ICompletableFuture<Boolean> confirmationResult = new CompletableFuture<>();
            futures.add(confirmationResult);
            confirmationResult.thenAccept(res -> {
                // Immediately restart when a query is invalidated
                if(!res) {
                    result.complete(Optional.empty());
                }
            });
            final S scope = rq.scopePath().getTarget();
            context.match(scope).whenComplete((m, ex) -> {
                if(ex != null) {
                    if(ex == Release.instance) {
                        confirmationResult.complete(true);
                    } else {
                        confirmationResult.completeExceptionally(ex);
                    }
                } else if(!m.isPresent()) {
                    confirmationResult.complete(rq.result().isEmpty());
                } else {
                    final IFuture<IQueryAnswer<S, L, D>> queryResult = context.query(new ScopePath<>(m.get()), rq.labelWf(), rq.labelOrder(), rq.dataWf(), rq.dataLeq());
                    queryResult.whenComplete((env, ex2) -> {
                        if(ex2 != null) {
                            if(ex2 == Release.instance) {
                                confirmationResult.complete(true);
                            } else {
                                confirmationResult.completeExceptionally(ex2);
                            }
                        } else {
                            // Query is valid iff environments are equal
                            // TODO: compare environments with scope patches.
                            java.util.Set<ResolutionPath<S, L, D>> oldPaths = Sets.newHashSet(rq.result());
                            java.util.Set<ResolutionPath<S, L, D>> newPaths = Sets.newHashSet(env.env());
                            confirmationResult.complete(oldPaths.equals(newPaths));
                        }
                    });
                }
            });
        });

        Futures.noneMatch(futures, p -> p.thenApply(v -> !v))
            .thenAccept(confirmed -> {
                if(confirmed) {
                    result.complete(Optional.of(BiMap.Immutable.of()));
                } else {
                    result.complete(Optional.empty());
                }
            });

        return result;
    }

    @Override public IFuture<Optional<BiMap.Immutable<S>>> confirm(ScopePath<S, L> path, LabelWf<L> labelWF,
            DataWf<S, L, D> dataWF, boolean prevEnvEmpty) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    public static <S, L, D> IConfirmationFactory<S, L, D> factory() {
        return TrivialConfirmation::new;
    }

}