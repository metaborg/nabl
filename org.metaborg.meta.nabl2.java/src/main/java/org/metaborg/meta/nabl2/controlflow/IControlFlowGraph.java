package org.metaborg.meta.nabl2.controlflow;

import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation2;
import org.metaborg.meta.nabl2.util.collections.ISet;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

public interface IControlFlowGraph<S extends ICFGNode> {

    ISet<S> getAllCFGNodes();

    ISet<S> getAllStarts();

    ISet<S> getAllEnds();


    IFunction<Tuple2<S, String>, Object> getProperties();

    IFunction<Tuple2<S, String>, Integer> getTFNumbers();

    IRelation2<S, S> getDirectEdges();
}