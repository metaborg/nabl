package org.metaborg.meta.nabl2.controlflow.terms;

import java.io.Serializable;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.Tuple2;

import io.usethesource.capsule.Map;

@Immutable
public abstract class FlowSpecSolution<N extends ICFGNode> implements IFlowSpecSolution<N>, Serializable {
    @Override
    @Parameter
    public abstract ICompleteControlFlowGraph.Immutable<N> controlFlowGraph();

    @Override
    @Parameter
    public abstract Map.Immutable<Tuple2<TermIndex, String>, ITerm> preProperties();

    @Override
    @Parameter
    public abstract Map.Immutable<Tuple2<TermIndex, String>, ITerm> postProperties();

    public static <N extends ICFGNode> IFlowSpecSolution<N> of(ICompleteControlFlowGraph.Immutable<N> controlFlowGraph) {
        return ImmutableFlowSpecSolution.of(controlFlowGraph, Map.Immutable.of(), Map.Immutable.of());
    }

    public static <N extends ICFGNode> IFlowSpecSolution<N> of() {
        return ImmutableFlowSpecSolution.of(ControlFlowGraph.<N>of().asCompleteControlFlowGraph(), Map.Immutable.of(), Map.Immutable.of());
    }
}
