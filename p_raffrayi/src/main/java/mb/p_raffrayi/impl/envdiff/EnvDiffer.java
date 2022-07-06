package mb.p_raffrayi.impl.envdiff;

import java.util.ArrayList;
import java.util.List;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action4;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.impl.diff.IDifferOps;
import mb.p_raffrayi.impl.diff.ScopeDiff;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.Edge;

public class EnvDiffer<S, L, D> implements IEnvDiffer<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(EnvDiffer.class);

    private final IDifferOps<S, L, D> differOps;
    private final IEnvDifferContext<S, L, D> context;

    public EnvDiffer(IEnvDifferContext<S, L, D> context, IDifferOps<S, L, D> differOps) {
        this.differOps = differOps;
        this.context = context;
    }

    @Override public IFuture<IEnvDiff<S, L, D>> diff(S scope, LabelWf<L> labelWf) {
        return diff(scope, CapsuleUtil.immutableSet(scope), labelWf);
    }

    private IFuture<IEnvDiff<S, L, D>> diff(S scope, Set.Immutable<S> seenScopes, LabelWf<L> labelWf) {
        logger.debug("Computing env diff for {} ~ {}.", scope, labelWf);
        if(!differOps.ownScope(scope)) {
            logger.debug("{} external", scope);
            return CompletableFuture.completedFuture(EnvDiffs.empty());
        }

        return context.match(scope).thenCompose(match_opt -> {
            return match_opt.map(currentScope -> {
                logger.debug("{} matched", scope);
                final List<IFuture<ScopeDiff<S, L, D>>> futures = new ArrayList<>();
                for(L label : this.context.edgeLabels()) {
                    if(labelWf.step(label).isPresent()) {
                        futures.add(context.scopeDiff(scope, label));
                    }
                }

                return AggregateFuture.of(futures).thenCompose(diffs -> {
                    final ArrayList<IFuture<Unit>> subEnvFutures = new ArrayList<>();
                    final EnvDiffBuilder<S, L, D> envDiffBuilder = new EnvDiffBuilder<>(scope, currentScope);
                    for(ScopeDiff<S, L, D> diff : diffs) {
                        // Process all added/removed edges
                        // @formatter:off
                        traverseApplicable(diff.addedEdges(), labelWf, seenScopes, (label, target, newSeenScopes, newLabelWf) -> {
                            logger.debug("{} -{}-> {} added", scope, label, target);
                            envDiffBuilder.addChange(AddedEdge.of(target, newLabelWf));
                        });
                        traverseApplicable(diff.removedEdges(), labelWf, seenScopes, (label, target, newSeenScopes, newLabelWf) -> {
                            logger.debug("{} -{}-> {} removed", scope, label, target);
                            envDiffBuilder.addChange(RemovedEdge.of(target, newLabelWf));
                        });

                        // Asynchronously collect all sub environment diffs
                        traverseApplicable(diff.matchedEdges(), labelWf, seenScopes, (label, target, newSeenScopes, newLabelWf) -> {
                            logger.debug("{} -{}-> {} matched. Computing difftree step.", scope, label, target);
                            subEnvFutures.add(diff(target, newSeenScopes, newLabelWf).thenApply(subDiff -> {
                                envDiffBuilder.addEnvDiff(subDiff);
                                return Unit.unit;
                            }));
                        });
                        // @formatter:on
                    }

                    return AggregateFuture.of(subEnvFutures).thenApply(__ -> {
                        logger.debug("env diff for {} ~ {} complete.", scope, labelWf);
                        final IEnvDiff<S, L, D> diffTree = envDiffBuilder.build();
                        logger.trace("diff value: {}", diffTree);
                        return diffTree;
                    });
                });
            }).orElseGet(() -> {
                logger.debug("{} removed", scope);
                final Set.Immutable<IEnvChange<S, L, D>> change = CapsuleUtil.immutableSet(RemovedEdge.of(scope, labelWf));
                return CompletableFuture.completedFuture(EnvDiff.of(BiMap.Immutable.of(), change));
            });
        });
    }

    private void traverseApplicable(Iterable<Edge<S, L>> edges, LabelWf<L> labelWf, Set.Immutable<S> seenScopes,
            Action4<L, S, Set.Immutable<S>, LabelWf<L>> action) {
        edges.forEach(edge -> {
            // When label would be traversed by label WF ...
            labelWf.step(edge.label).ifPresent(newLabelWf -> {
                final Set.Transient<S> newSeenScopes = seenScopes.asTransient();
                // ... and target scope is not seen yet ...
                if(newSeenScopes.__insert(edge.target)) {
                    // ... apply action
                    action.apply(edge.label, edge.target, newSeenScopes.freeze(), newLabelWf);
                }
            });
        });

    }

}
