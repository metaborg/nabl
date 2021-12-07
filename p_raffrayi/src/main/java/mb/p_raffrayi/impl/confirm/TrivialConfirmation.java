package mb.p_raffrayi.impl.confirm;

import java.util.ArrayList;
import java.util.List;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.Futures;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.impl.IQueryAnswer;
import mb.p_raffrayi.impl.Release;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;
import mb.scopegraph.patching.PatchCollection;

// TODO: remove completely
public class TrivialConfirmation<S, L, D> implements IConfirmation<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(TrivialConfirmation.class);

    private final IConfirmationContext<S, L, D> context;

    public TrivialConfirmation(IConfirmationContext<S, L, D> context) {
        this.context = context;
    }

    @Override public IFuture<ConfirmResult<S>> confirm(java.util.Set<IRecordedQuery<S, L, D>> queries) {
        final ICompletableFuture<ConfirmResult<S>> result = new CompletableFuture<>();

        final List<IFuture<Boolean>> futures = new ArrayList<>();
        queries.forEach(rq -> {
            logger.debug("Confirming {}.", rq);
            final ICompletableFuture<Boolean> confirmationResult = new CompletableFuture<>();
            futures.add(confirmationResult);
            confirmationResult.thenAccept(res -> {
                // Immediately restart when a query is invalidated
                if(!res) {
                    logger.debug("{} denied, eagerly propagating", rq);
                    result.complete(ConfirmResult.deny());
                }
            });
            final S scope = rq.scopePath().getTarget();
            context.match(scope).whenComplete((m, ex) -> {
                if(ex != null) {
                    if(ex == Release.instance) {
                        logger.debug("Unit released, propagating accept.");
                        confirmationResult.complete(true);
                    } else {
                        logger.error("Confirmation completed exceptionally.", ex);
                        confirmationResult.completeExceptionally(ex);
                    }
                } else if(!m.isPresent()) {
                    logger.error("No match for {}. Confirming if previous environment empty.", scope);
                    confirmationResult.complete(rq.empty());
                } else {
                    logger.debug("Matched {} ~ {}. Confirming if query in match returns equal results.", m.get(), scope);
                    final IFuture<IQueryAnswer<S, L, D>> queryResult = context.query(new ScopePath<>(m.get()),
                            rq.labelWf(), LabelOrder.none(), rq.dataWf(), DataLeq.none());
                    queryResult.whenComplete((env, ex2) -> {
                        if(ex2 != null) {
                            if(ex2 == Release.instance) {
                                logger.debug("Unit released, propagating accept.");
                                confirmationResult.complete(true);
                            } else {
                                logger.error("Confirmation completed exceptionally.", ex2);
                                confirmationResult.completeExceptionally(ex2);
                            }
                        } else {
                            logger.debug("Denied {} because no previous environment given.", rq);
                            confirmationResult.complete(false);
                        }
                    });
                }
            });
        });

        Futures.noneMatch(futures, p -> p.thenApply(v -> !v)).thenAccept(confirmed -> {
            if(confirmed) {
                logger.debug("All queries confirmed.");
                result.complete(ConfirmResult.confirm(PatchCollection.Immutable.of()));
            } else {
                logger.debug("Some queries denied, propagating denial.");
                result.complete(ConfirmResult.deny());
            }
        });

        return result;
    }

    @Override public IFuture<ConfirmResult<S>> confirm(ScopePath<S, L> path, LabelWf<L> labelWf, DataWf<S, L, D> dataWf,
            boolean prevEnvEmpty) {
        logger.debug("Denying query {}/{}.", path.getTarget(), labelWf);
        return CompletableFuture.completedFuture(ConfirmResult.deny());
    }

    public static <S, L, D> IConfirmationFactory<S, L, D> factory() {
        return TrivialConfirmation::new;
    }

}
