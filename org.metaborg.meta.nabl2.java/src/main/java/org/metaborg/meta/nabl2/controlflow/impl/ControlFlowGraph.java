package org.metaborg.meta.nabl2.controlflow.impl;

import org.metaborg.meta.nabl2.controlflow.ICFGNode;
import org.metaborg.meta.nabl2.controlflow.IControlFlowGraph;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.util.collections.*;

import java.io.Serializable;

public class ControlFlowGraph<S extends ICFGNode, O extends IOccurrence>
    implements IControlFlowGraph<S, O>, Serializable {

    private final ISet.Mutable<S> allCFGNodes;
    private final ISet.Mutable<O> allDecls;

    private final IFunction.Mutable<O, S> decls;
    private final IRelation2.Mutable<S, S> directEdges;

    public ControlFlowGraph() {
        this.allCFGNodes = HashSet.create();
        this.allDecls = HashSet.create();

        this.decls = HashFunction.create();
        this.directEdges = HashRelation2.create();
    }

    @Override
    public ISet<S> getAllCFGNodes() {
        return allCFGNodes;
    }

    @Override
    public ISet<S> getAllStarts() {
        throw new RuntimeException("unimplemented");
    }

    @Override
    public ISet<S> getAllEnds() {
        throw new RuntimeException("unimplemented");
    }

    @Override
    public IFunction<O, S> getDecls() {
        return decls;
    }

    @Override
    public IRelation2<S, S> getDirectEdges() {
        return directEdges;
    }

    @Override
    public ISet<O> getAllDecls() {
        return allDecls;
    }

    public void addDecl(S node, O decl) {
        allCFGNodes.add(node);
        allDecls.add(decl);
        decls.put(decl, node);
    }

    public void addDirectEdge(S sourceNode, S targetNode) {
        allCFGNodes.add(sourceNode);
        directEdges.put(sourceNode, targetNode);
    }
}
