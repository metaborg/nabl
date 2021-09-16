package mb.p_raffrayi.impl.confirm;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.future.CompletableFuture;

import mb.p_raffrayi.IRecordedQuery;
import mb.scopegraph.oopsla20.diff.BiMap;

public class LazyConfirmation<S, L, D> extends OptimisticConfirmation<S, L, D> {

    static final ILogger logger = LoggerUtils.logger(LazyConfirmation.class);

    public LazyConfirmation(IConfirmationContext<S, L, D> context) {
        super(context);
    }

    @Override public IFuture<ConfirmResult<S>> confirm(IRecordedQuery<S, L, D> query) {
        // First confirm root query
        logger.debug("Locally confirming root query: {}.", query);
        return confirmSingle(query).thenCompose(res -> {
            logger.debug("Local confirmation result: {}.", res);
            return mapConfirmResultToFuture(res, initialPatches -> {
                // If that succeeds, confirm all transitive queries
                logger.debug("Local query confirmed; confirming transitive queries: {}.", query);
                return confirm(query.transitiveQueries()).thenCompose(res2 -> {
                    logger.debug("Transitive confirmation result: {}.", res2);
                    return mapConfirmResultToFuture(res2, transitivePatches -> {
                        // If that succeeds, confirm all queries raised by predicates
                        logger.debug("Transitive query confirmed; confirming predicate queries: {}.", query);
                        return confirm(query.predicateQueries()).thenApply(res3 -> {
                            logger.debug("Predicate query confirmation result: {}.", res3);
                            return mapConfirmResult(res3, __ -> {
                                logger.debug("Aggregating local and transitive patches.");
                                // Do no include patches from predicate queries in eventual result, because
                                // nested state should not leak to the outer state, and hence plays no role in confirmation.
                                return initialPatches.putAll(transitivePatches);
                            });
                        });
                    });
                });
            });
        });
    }

    private IFuture<ConfirmResult<S>> mapConfirmResultToFuture(ConfirmResult<S> res,
            Function1<BiMap.Immutable<S>, IFuture<ConfirmResult<S>>> mapper) {
        return res.match(() -> CompletableFuture.completedFuture(ConfirmResult.deny()), mapper);
    }

    private ConfirmResult<S> mapConfirmResult(ConfirmResult<S> res,
            Function1<BiMap.Immutable<S>, BiMap.Immutable<S>> mapper) {
        return res.match(() -> ConfirmResult.deny(), patches -> ConfirmResult.confirm(mapper.apply(patches)));
    }

    public static <S, L, D> IConfirmationFactory<S, L, D> factory() {
        return LazyConfirmation::new;
    }

}