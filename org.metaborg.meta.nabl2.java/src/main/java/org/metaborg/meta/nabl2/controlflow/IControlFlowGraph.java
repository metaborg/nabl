package org.metaborg.meta.nabl2.controlflow;

import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation2;
import org.metaborg.meta.nabl2.util.collections.ISet;

public interface IControlFlowGraph<S extends ICFGNode, O extends IOccurrence> {

    ISet<S> getAllCFGNodes();

    ISet<S> getAllStarts();

    ISet<S> getAllEnds();

    ISet<O> getAllDecls();


    IFunction<O, S> getDecls();

    IRelation2<S, S> getDirectEdges();
}