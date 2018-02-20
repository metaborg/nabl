package org.metaborg.meta.nabl2.controlflow.terms;

import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.Tuple2;

import io.usethesource.capsule.Map;

public interface IFlowSpecSolution<N extends ICFGNode> {
    ICompleteControlFlowGraph.Immutable<N> controlFlowGraph();
    Map.Immutable<Tuple2<TermIndex, String>, ITerm> preProperties();
    Map.Immutable<Tuple2<TermIndex, String>, ITerm> postProperties();
    
    IFlowSpecSolution<N> withPreProperties(Map.Immutable<Tuple2<TermIndex, String>, ITerm> value);
    IFlowSpecSolution<N> withPostProperties(Map.Immutable<Tuple2<TermIndex, String>, ITerm> value);
}
