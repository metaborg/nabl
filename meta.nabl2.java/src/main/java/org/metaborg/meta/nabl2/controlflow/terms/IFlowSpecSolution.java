package org.metaborg.meta.nabl2.controlflow.terms;

import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.Tuple2;

import io.usethesource.capsule.Map;

public interface IFlowSpecSolution<N extends ICFGNode> {
    ICompleteControlFlowGraph.Immutable<N> controlFlowGraph();
    // TODO: change to Map.Immutable<String, Map<TermIndex, ITerm>>?
    Map.Immutable<Tuple2<TermIndex, String>, ITerm> preProperties();
    Map.Immutable<Tuple2<TermIndex, String>, ITerm> postProperties();
    /**
     * @return The transfer functions associated with each node in the control flow graph(s) by property. 
     */
    Map.Immutable<Tuple2<TermIndex, String>, TransferFunctionAppl> tfAppls();

    default TransferFunctionAppl getTFAppl(N node, String prop) {
        return tfAppls().get(ImmutableTuple2.of(TermIndex.get(node).get(), prop));
    }

    IFlowSpecSolution<N> withPreProperties(Map.Immutable<Tuple2<TermIndex, String>, ITerm> value);
    IFlowSpecSolution<N> withPostProperties(Map.Immutable<Tuple2<TermIndex, String>, ITerm> value);
}
