package org.metaborg.meta.nabl2.controlflow.terms;

import org.immutables.value.Value;
import org.immutables.value.Value.Lazy;
import org.immutables.value.Value.Parameter;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Set;

@Value.Immutable
public abstract class TransientControlFlowGraph<N extends ICFGNode> implements IControlFlowGraph.Transient<N> {
    @Override
    @Lazy
    public Set<N> nodes() {
        Set.Transient<N> allNodes = Set.Transient.of();
        allNodes.__insertAll(startNodes());
        allNodes.__insertAll(normalNodes());
        allNodes.__insertAll(artificialNodes());
        allNodes.__insertAll(endNodes());
        return allNodes.freeze();
    }
    
    @Override
    @Parameter
    public abstract BinaryRelation.Transient<N, N> edges();

    @Override
    @Parameter
    public abstract Set.Transient<N> startNodes();

    @Override
    @Parameter
    public abstract Set.Transient<N> endNodes();

    @Override
    @Parameter
    public abstract Set.Transient<N> normalNodes();

    @Override
    @Parameter
    public abstract Set.Transient<N> artificialNodes();

    @Override
    public IControlFlowGraph.Immutable<N> freeze() {
        return ImmutableControlFlowGraph.of(edges().freeze(), startNodes().freeze(), endNodes().freeze(), normalNodes().freeze(), artificialNodes().freeze());
    }

    public static <N extends ICFGNode> IControlFlowGraph.Transient<N> of() {
        return ImmutableTransientControlFlowGraph.of(BinaryRelation.Transient.of(), Set.Transient.of(), Set.Transient.of(),
                Set.Transient.of(), Set.Transient.of());
    }

    public static <N extends ICFGNode> IControlFlowGraph.Transient<N> from(ICompleteControlFlowGraph.Immutable<N> cfg) {
        IControlFlowGraph.Transient<N> result = of();
        result.addAll(cfg);
        return result;
    }
}
