package org.metaborg.meta.nabl2.controlflow.terms;

import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

public interface IControlFlowGraph<N extends ICFGNode> {

    Set<N> getAllNodes();

    Set<N> getStartNodes();

    Set<N> getEndNodes();

    Set<N> getNormalNodes();

    Set<N> getArtificialNodes();


    Map<Tuple2<N, String>, ITerm> getProperties();

    Map<Tuple2<TermIndex, String>, TransferFunctionAppl> getTFAppls();

    BinaryRelation<N, N> getDirectEdges();

    Object getProperty(N node, String prop);

    boolean isEmpty();

    void complete();
}