package mb.p_raffrayi.impl.envdiff;

import java.util.ArrayList;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action4;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.impl.diff.IDifferOps;
import mb.p_raffrayi.impl.diff.IScopeGraphDiffer;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.diff.Edge;

public class EnvDiffer<S, L, D> implements IEnvDiffer<S, L, D> {

    private final IDifferOps<S, L, D> differOps;
    private final IScopeGraphDiffer<S, L, D> scopeGraphDiffer;

    public EnvDiffer(IScopeGraphDiffer<S, L, D> scopeGraphDiffer, IDifferOps<S, L, D> differOps) {
        this.differOps = differOps;
        this.scopeGraphDiffer = scopeGraphDiffer;
    }

    @Override public IFuture<IEnvDiff<S, L, D>> diff(S scope, LabelWf<L> labelWf, DataWf<S, L, D> dataWf) {
        return diff(scope, CapsuleUtil.immutableSet(scope), labelWf, dataWf);
    }

    @Override public IFuture<IEnvDiff<S, L, D>> diff(S scope, Set.Immutable<S> seenScopes, LabelWf<L> labelWf,
            DataWf<S, L, D> dataWf) {
        if(!differOps.ownScope(scope) ) {
            return CompletableFuture.completedFuture(External.of(scope, seenScopes, labelWf, dataWf));
        }

        return scopeGraphDiffer.scopeDiff(scope).thenCompose(scopeDiff -> {
            return scopeDiff.<IFuture<IEnvDiff<S, L, D>>>match(match -> {
                final DiffTreeBuilder<S, L, D> treeBuilder = new DiffTreeBuilder<>(scope, match.currentScope());

                // Process all added/removed edges
                // @formatter:off
                traverseApplicable(match.addedEdges(), labelWf, seenScopes, (label, target, newSeenScopes, newLabelWf) -> {
                    treeBuilder.addSubTree(label, target, AddedEdge.of(target, newSeenScopes, newLabelWf, dataWf));
                });
                traverseApplicable(match.removedEdges(), labelWf, seenScopes, (label, target, newSeenScopes, newLabelWf) -> {
                    treeBuilder.addSubTree(label, target, RemovedEdge.of(target, newLabelWf, dataWf));
                });

                // Asynchronously collect all sub environment diffs
                final ArrayList<IFuture<Unit>> subEnvFutures = new ArrayList<>();
                traverseApplicable(match.matchedEdges(), labelWf, seenScopes, (label, target, newSeenScopes, newLabelWf) -> {
                    subEnvFutures.add(diff(target, newSeenScopes, newLabelWf, dataWf).thenApply(subDiff -> {
                        treeBuilder.addSubTree(label, target, subDiff);
                        return Unit.unit;
                    }));
                });
                // @formatter:on

                return new AggregateFuture<>(subEnvFutures).thenApply(__ -> treeBuilder.build());
            }, () -> CompletableFuture.completedFuture(RemovedEdge.of(scope, labelWf, dataWf)));
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
