package mb.nabl2.scopegraph.esop.bottomup;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.time.AggregateTimer;

import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.ISolution;

public class BUVerifier {

    private static ILogger logger = LoggerUtils.logger(BUVerifier.class);

    public static void verify(ISolution solution) {
        logger.info("resolving with bottom-up resolution");
        AggregateTimer timer = new AggregateTimer(false);
        try {
            final BUNameResolution<Scope, Label, Occurrence> nameResolution =
                    BUNameResolution.of(solution.scopeGraph(), solution.config().getResolutionParams());
            timer.start();
            logger.info("verifying {} resolve entries", solution.nameResolutionCache().resolutionEntries().size());
            for(Entry<Occurrence, Collection<IResolutionPath<Scope, Label, Occurrence>>> entry : solution
                    .nameResolutionCache().resolutionEntries().entrySet()) {
                final Collection<IResolutionPath<Scope, Label, Occurrence>> result =
                        nameResolution.resolve(entry.getKey());
                if(!result.equals(entry.getValue())) {
                    logger.warn("resolve {} failed, got {}, expected {}", entry.getKey(), result, entry.getValue());
                }
            }
            logger.info("verifying {} visible entries", solution.nameResolutionCache().visibilityEntries().size());
            for(Entry<Scope, Collection<Occurrence>> entry : solution.nameResolutionCache().visibilityEntries()
                    .entrySet()) {
                final Collection<Occurrence> result = nameResolution.visible(entry.getKey());
                if(!result.equals(entry.getValue())) {
                    logger.warn("visible {} failed, got {}, expected {}", entry.getKey(), result, entry.getValue());
                }
            }
            logger.info("verifying {} reachable entries", solution.nameResolutionCache().reachabilityEntries().size());
            for(Entry<Scope, Collection<Occurrence>> entry : solution.nameResolutionCache().reachabilityEntries()
                    .entrySet()) {
                final Collection<Occurrence> result = nameResolution.reachable(entry.getKey());
                if(!result.equals(entry.getValue())) {
                    logger.warn("reachable {} failed, got {}, expected {}", entry.getKey(), result, entry.getValue());
                }
            }
        } catch(Exception e) {
            logger.error("bottom-up resolution failed", e);
        } finally {
            timer.stop();
        }
        logger.info("bottom-up resolution took {} s", TimeUnit.SECONDS.convert(timer.total(), TimeUnit.NANOSECONDS));
    }

}