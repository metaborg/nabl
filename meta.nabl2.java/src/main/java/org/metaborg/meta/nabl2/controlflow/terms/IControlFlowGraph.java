package org.metaborg.meta.nabl2.controlflow.terms;

import java.util.Collection;
import java.util.List;

import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.Tuple2;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;

public interface IControlFlowGraph<N extends ICFGNode> {

    Set<N> getAllNodes();

    Set<N> getStartNodes();

    Set<N> getEndNodes();

    Set<N> getNormalNodes();

    Set<N> getArtificialNodes();


    Map<Tuple2<N, String>, ITerm> getProperties();

    Map<Tuple2<TermIndex, String>, TransferFunctionAppl> getTFAppls();

    BinaryRelation<N, N> getDirectEdges();

    ITerm getProperty(N node, String prop);

    boolean isEmpty();

    void complete();

    List<N> getUnreachableNodes();

    Collection<java.util.Set<N>> getTopoSCCs();

    Collection<java.util.Set<N>> getRevTopoSCCs();
}