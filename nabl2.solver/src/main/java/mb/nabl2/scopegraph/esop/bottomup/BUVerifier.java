package mb.nabl2.scopegraph.esop.bottomup;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.time.AggregateTimer;

import io.usethesource.capsule.Set;
import mb.nabl2.scopegraph.CriticalEdgeException;
import mb.nabl2.scopegraph.StuckException;
import mb.nabl2.scopegraph.esop.IEsopNameResolution;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph.Immutable;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;

public class BUVerifier {

    private static ILogger logger = LoggerUtils.logger(BUVerifier.class);

    public static boolean verify(ISolution solution, ICancel cancel, IProgress progress) {
        boolean success = true;
        try {
            logger.info("resolving with bottom-up resolution");
            AggregateTimer timer = new AggregateTimer(false);
            final IEsopNameResolution<Scope, Label, Occurrence> nameResolution = solution.nameResolution();
            final Immutable<Scope, Label, Occurrence, ITerm> scopeGraph = solution.scopeGraph();
            final BUNameResolution<Scope, Label, Occurrence> buNameResolution =
                    BUNameResolution.of(solution.config().getResolutionParams(), scopeGraph, (s, l) -> false);
            logger.info("verifying {} references", scopeGraph.getAllRefs().size());
            for(Occurrence ref : scopeGraph.getAllRefs()) {
                try {
                    final Collection<IResolutionPath<Scope, Label, Occurrence>> paths = nameResolution.resolve(ref, cancel, progress);
                    timer.start();
                    final Collection<IResolutionPath<Scope, Label, Occurrence>> result = buNameResolution.resolve(ref, cancel, progress);
                    timer.stop();
                    success &= verifyEquals("resolve " + ref, paths, result, buNameResolution);
                } catch(CriticalEdgeException ex) {
                    timer.stop();
                    logger.error("[resolve {}] incomplete", ex, ref);
                    success &= false;
                } catch(StuckException ex) {
                    timer.stop();
                    logger.error("[resolve {}] stuck", ex, ref);
                    success &= false;
                }
            }
            logger.info("verifying {} visible", scopeGraph.getAllScopes().size());
            for(Scope scope : scopeGraph.getAllScopes()) {
                try {
                    final Collection<Occurrence> decls = nameResolution.visible(scope, cancel, progress);
                    timer.start();
                    final Collection<Occurrence> result = buNameResolution.visible(scope, cancel, progress);
                    timer.stop();
                    success &= verifyEquals("visible " + scope, decls, result, buNameResolution);
                } catch(CriticalEdgeException ex) {
                    timer.stop();
                    logger.error("[visible {}] incomplete", ex, scope);
                    success &= false;
                } catch(StuckException ex) {
                    timer.stop();
                    logger.error("[visible {}] stuck", scope);
                    success &= false;
                }
            }
            logger.info("verifying {} reachable", scopeGraph.getAllScopes());
            for(Scope scope : scopeGraph.getAllScopes()) {
                try {
                    final Collection<Occurrence> decls = nameResolution.reachable(scope, cancel, progress);
                    timer.start();
                    final Collection<Occurrence> result = buNameResolution.reachable(scope, cancel, progress);
                    timer.stop();
                    success &= verifyEquals("reachable " + scope, decls, result, buNameResolution);
                } catch(CriticalEdgeException ex) {
                    timer.stop();
                    logger.error("[reachable {}] incomplete", ex, scope);
                    success &= false;
                } catch(StuckException ex) {
                    timer.stop();
                    logger.error("[reachable {}] stuck", scope);
                    success &= false;
                }
            }
            logger.info("bottom-up resolution took {} s",
                    (double) timer.total() / (double) TimeUnit.NANOSECONDS.convert(1l, TimeUnit.SECONDS));
        } catch(InterruptedException e) {
            logger.error("bottom-up resolution interrupted", e);
            success = false;
        }
        return success;
    }

    private static <E> boolean verifyEquals(String tag, Collection<E> expected, Collection<E> actual,
            BUNameResolution<Scope, Label, Occurrence> bu) {
        final Set.Immutable<E> expectedSet = CapsuleUtil.toSet(expected);
        final Set.Immutable<E> actualSet = CapsuleUtil.toSet(actual);
        final Set.Immutable<E> missing = Set.Immutable.subtract(expectedSet, actualSet);
        final Set.Immutable<E> extra = Set.Immutable.subtract(actualSet, expectedSet);
        if(!missing.isEmpty() || !extra.isEmpty()) {
            final Set.Immutable<E> matched = Set.Immutable.intersect(actualSet, expectedSet);
            logger.info("[{}] {} matched {}", tag, matched.size(), matched);
        }
        if(!missing.isEmpty()) {
            logger.error("[{}] {} missing {}", tag, missing.size(), missing);
        }
        if(!extra.isEmpty()) {
            logger.error("[{}] {} extra {}", tag, extra.size(), extra);
        }
        return missing.isEmpty() && extra.isEmpty();
    }

}