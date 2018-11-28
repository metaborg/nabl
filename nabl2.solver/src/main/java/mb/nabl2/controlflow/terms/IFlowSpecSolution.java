package mb.nabl2.controlflow.terms;

import javax.annotation.Nullable;

import org.metaborg.util.Ref;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public interface IFlowSpecSolution<N extends ICFGNode> {
    ICompleteControlFlowGraph.Immutable<N> controlFlowGraph();
    // TODO: change to Map.Immutable<String, Map<CFGNode, ITerm>>?
    Map.Immutable<Tuple2<CFGNode, String>, Ref<ITerm>> preProperties();
    Map.Immutable<Tuple2<CFGNode, String>, Ref<ITerm>> postProperties();
    /**
     * @return The transfer functions associated with each node in the control flow graph(s) by property. 
     */
    Map.Immutable<Tuple2<CFGNode, String>, TransferFunctionAppl> tfAppls();

    default @Nullable TransferFunctionAppl getTFAppl(CFGNode node, String prop) {
        return tfAppls().get(ImmutableTuple2.of(node, prop));
    }

    IFlowSpecSolution<N> withPreProperties(Map.Immutable<Tuple2<CFGNode, String>, Ref<ITerm>> value);
    IFlowSpecSolution<N> withPostProperties(Map.Immutable<Tuple2<CFGNode, String>, Ref<ITerm>> value);
}
