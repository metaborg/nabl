package org.metaborg.meta.nabl2.controlflow.terms;

import java.io.Serializable;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Lazy;
import org.immutables.value.Value.Parameter;
import org.metaborg.meta.nabl2.controlflow.terms.Algorithms.TopoSCCResult;
import org.metaborg.meta.nabl2.util.Tuple2;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;

@org.immutables.value.Value.Immutable
public abstract class CompleteControlFlowGraph<N extends ICFGNode>
        implements ICompleteControlFlowGraph.Immutable<N>, Serializable {
    @Override
    @Lazy
    public Set.Immutable<N> nodes() {
        Set.Transient<N> allNodes = Set.Transient.of();
        allNodes.__insertAll(startNodes());
        allNodes.__insertAll(normalNodes());
        allNodes.__insertAll(endNodes());
        return allNodes.freeze();
    }

    @Override
    @Parameter
    public abstract BinaryRelation.Immutable<N, N> edges();

    @Override
    @Parameter
    public abstract Set.Immutable<N> startNodes();

    @Override
    @Parameter
    public abstract Set.Immutable<N> normalNodes();

    @Override
    @Parameter
    public abstract Set.Immutable<N> endNodes();

    @Override
    @Parameter
    public abstract Map.Immutable<Tuple2<N, String>, TransferFunctionAppl> tfAppls();

    @Override
    @Auxiliary
    @Parameter
    public abstract Iterable<N> unreachableNodes();

    @Override
    @Auxiliary
    @Parameter
    public abstract Iterable<java.util.Set<N>> topoSCCs();

    @Override
    @Auxiliary
    @Parameter
    public abstract Iterable<java.util.Set<N>> revTopoSCCs();

    public static <N extends ICFGNode> ICompleteControlFlowGraph.Immutable<N> of(Set.Immutable<N> normalNodes,
            BinaryRelation.Immutable<N, N> edges, Set.Immutable<N> startNodes, Set.Immutable<N> endNodes,
            Map.Immutable<Tuple2<N, String>, TransferFunctionAppl> tfAppls) {
        /*
         * TODO: can we do better? SCCs are the same, topo order can be reversed, just
         * the order within the SCCs needs to be different. Perhaps faster to do
         * ordering within SCCs as post processing?
         */

        Set.Immutable<N> allNodes = normalNodes.__insertAll(startNodes).__insertAll(endNodes);
        TopoSCCResult<N> result = Algorithms.topoSCCs(allNodes, startNodes, endNodes, edges);

        return ImmutableCompleteControlFlowGraph.of(edges, startNodes, normalNodes, endNodes, tfAppls,
                result.unreachables(), result.topoSCCs(), result.revTopoSCCs());
    }
}
