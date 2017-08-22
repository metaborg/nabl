package org.metaborg.meta.nabl2.controlflow;

import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation2;
import org.metaborg.meta.nabl2.util.collections.ISet;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import meta.flowspec.java.interpreter.TransferFunctionAppl;

public interface IControlFlowGraph<S extends ICFGNode> {

    ISet<S> getAllCFGNodes();

    ISet<S> getAllStarts();

    ISet<S> getAllEnds();


    IFunction<Tuple2<S, String>, Object> getProperties();

    IFunction<Tuple2<S, String>, TransferFunctionAppl> getTFAppls();

    IRelation2<S, S> getDirectEdges();
}