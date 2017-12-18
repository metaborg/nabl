package org.metaborg.meta.nabl2.controlflow.terms;

import java.io.Serializable;

import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

public class ControlFlowGraph<N extends ICFGNode>
    implements IControlFlowGraph<N>, Serializable {

    private final Set.Transient<N> allCFGNodes;

    private final Map.Transient<Tuple2<TermIndex, String>, TransferFunctionAppl> tfAppls;
    private final Map.Transient<Tuple2<N, String>, ITerm> properties;
    private final BinaryRelation.Transient<N, N> directEdges;

    public ControlFlowGraph() {
        this.allCFGNodes = Set.Transient.of();

        this.tfAppls = Map.Transient.of();
        this.properties = Map.Transient.of();
        this.directEdges = BinaryRelation.Transient.of();
    }

    @Override
    public Set<N> getAllCFGNodes() {
        return allCFGNodes;
    }

    @Override
    public Set<N> getAllStarts() {
        throw new RuntimeException("unimplemented");
    }

    @Override
    public Set<N> getAllEnds() {
        throw new RuntimeException("unimplemented");
    }

    @Override
    public Map<Tuple2<TermIndex, String>, TransferFunctionAppl> getTFAppls() {
        return tfAppls;
    }

    @Override
    public Map<Tuple2<N, String>, ITerm> getProperties() {
        return properties;
    }

    @Override
    public BinaryRelation<N, N> getDirectEdges() {
        return directEdges;
    }

    public void addTFAppl(TermIndex index, String prop, TransferFunctionAppl tfAppl) {
        tfAppls.__put(ImmutableTuple2.of(index, prop), tfAppl);
    }

    public void setProperty(N node, String prop, ITerm value) {
        allCFGNodes.__insert(node);
        properties.__put(ImmutableTuple2.of(node, prop), value);
    }

    public TransferFunctionAppl getTFAppl(N node, String prop) {
        TransferFunctionAppl transferFunctionAppl = tfAppls.get(ImmutableTuple2.of(TermIndex.get(node), prop));
        if (transferFunctionAppl == null) {
            return new IdentityTFAppl<>(this, prop);
        } else {
            return transferFunctionAppl;
        }
    }

    @Override
    public Object getProperty(N node, String prop) {
        Object transferFunctionAppl = properties.get(ImmutableTuple2.of(node, prop));
        if (transferFunctionAppl == null) {
            return new IdentityTFAppl<>(this, prop);
        } else {
            return transferFunctionAppl;
        }
    }

    public void addDirectEdge(N sourceNode, N targetNode) {
        allCFGNodes.__insert(sourceNode);
        allCFGNodes.__insert(targetNode);
        directEdges.__insert(sourceNode, targetNode);
    }

    @Override
    public boolean isEmpty() {
        return directEdges.isEmpty();
    }

    public void addAll(IControlFlowGraph<N> controlFlowGraph) {
        this.allCFGNodes.__insertAll(controlFlowGraph.getAllCFGNodes());
        this.tfAppls.__putAll(controlFlowGraph.getTFAppls());
        controlFlowGraph.getDirectEdges().entryIterator().forEachRemaining(entry -> {
            this.directEdges.__insert(entry.getKey(), entry.getValue());
        });
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((allCFGNodes == null) ? 0 : allCFGNodes.hashCode());
        result = prime * result + ((directEdges == null) ? 0 : directEdges.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((tfAppls == null) ? 0 : tfAppls.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked")
        ControlFlowGraph<N> other = (ControlFlowGraph<N>) obj;
        if (allCFGNodes == null) {
            if (other.allCFGNodes != null)
                return false;
        } else if (!allCFGNodes.equals(other.allCFGNodes))
            return false;
        if (directEdges == null) {
            if (other.directEdges != null)
                return false;
        } else if (!directEdges.equals(other.directEdges))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (tfAppls == null) {
            if (other.tfAppls != null)
                return false;
        } else if (!tfAppls.equals(other.tfAppls))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ControlFlowGraph [allCFGNodes=" + allCFGNodes + ", tfAppls=" + tfAppls + ", properties=" + properties
                + ", directEdges=" + directEdges + "]";
    }
    
    public static <T extends ICFGNode> ControlFlowGraph<T> of() {
        return new ControlFlowGraph<>();
    }
}
