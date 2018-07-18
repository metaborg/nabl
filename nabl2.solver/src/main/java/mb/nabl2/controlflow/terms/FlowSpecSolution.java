package mb.nabl2.controlflow.terms;

import java.io.Serializable;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;

@Immutable
public abstract class FlowSpecSolution<N extends ICFGNode> implements IFlowSpecSolution<N>, Serializable {
    @Override
    @Parameter
    public abstract ICompleteControlFlowGraph.Immutable<N> controlFlowGraph();

    @Override
    @Parameter
    public abstract Map.Immutable<Tuple2<CFGNode, String>, TransferFunctionAppl> tfAppls();

    @Override
    @Parameter
    public abstract Map.Immutable<Tuple2<CFGNode, String>, ITerm> preProperties();

    @Override
    @Parameter
    public abstract Map.Immutable<Tuple2<CFGNode, String>, ITerm> postProperties();

    public static <N extends ICFGNode> IFlowSpecSolution<N> of(ICompleteControlFlowGraph.Immutable<N> controlFlowGraph, Map.Immutable<Tuple2<CFGNode, String>, TransferFunctionAppl> tfAppls) {
        return ImmutableFlowSpecSolution.of(controlFlowGraph, tfAppls, Map.Immutable.of(), Map.Immutable.of());
    }

    public static <N extends ICFGNode> IFlowSpecSolution<N> of() {
        return ImmutableFlowSpecSolution.of(CompleteControlFlowGraph.<N>of(), Map.Immutable.of(), Map.Immutable.of(), Map.Immutable.of());
    }
}
