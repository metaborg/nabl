package org.metaborg.meta.nabl2.controlflow.impl;

import java.io.Serializable;

import org.metaborg.meta.nabl2.controlflow.ICFGNode;
import org.metaborg.meta.nabl2.controlflow.IControlFlowGraph;
import org.metaborg.meta.nabl2.util.collections.HashFunction;
import org.metaborg.meta.nabl2.util.collections.HashRelation2;
import org.metaborg.meta.nabl2.util.collections.HashSet;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation2;
import org.metaborg.meta.nabl2.util.collections.ISet;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import meta.flowspec.java.interpreter.IdentityTFAppl;
import meta.flowspec.java.interpreter.TransferFunctionAppl;

public class ControlFlowGraph<S extends ICFGNode>
    implements IControlFlowGraph<S>, Serializable {

    private final ISet.Mutable<S> allCFGNodes;

    private final IFunction.Mutable<Tuple2<S, String>, TransferFunctionAppl> tfAppls;
    private final IFunction.Mutable<Tuple2<S, String>, Object> properties;
    private final IRelation2.Mutable<S, S> directEdges;

    public ControlFlowGraph() {
        this.allCFGNodes = HashSet.create();

        this.tfAppls = HashFunction.create();
        this.properties = HashFunction.create();
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
    public IFunction<Tuple2<S, String>, TransferFunctionAppl> getTFAppls() {
        return tfAppls;
    }

    @Override
    public IFunction<Tuple2<S, String>, Object> getProperties() {
        return properties;
    }

    @Override
    public IRelation2<S, S> getDirectEdges() {
        return directEdges;
    }

    public void addTFNumber(S node, String prop, TransferFunctionAppl tfAppl) {
        allCFGNodes.add(node);
        tfAppls.put(ImmutableTuple2.of(node, prop), tfAppl);
    }

    public void setProperty(S node, String prop, Object value) {
        allCFGNodes.add(node);
        properties.put(ImmutableTuple2.of(node, prop), value);
    }

    public TransferFunctionAppl getTFAppl(S node, String prop) {
        return tfAppls.get(ImmutableTuple2.of(node, prop)).orElse(new IdentityTFAppl<>(this, prop));
    }

    public Object getProperty(S node, String prop) {
        return properties.get(ImmutableTuple2.of(node, prop)).orElse(null);
    }

    public void addDirectEdge(S sourceNode, S targetNode) {
        allCFGNodes.add(sourceNode);
        directEdges.put(sourceNode, targetNode);
    }
}
