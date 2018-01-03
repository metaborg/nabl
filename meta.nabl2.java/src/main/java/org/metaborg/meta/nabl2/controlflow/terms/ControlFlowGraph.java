package org.metaborg.meta.nabl2.controlflow.terms;

import java.io.Serializable;
import java.util.Optional;

import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

public class ControlFlowGraph<N extends ICFGNode>
    implements IControlFlowGraph<N>, Serializable {

    private final Set.Transient<N> startNodes;
    private final Set.Transient<N> endNodes;
    private final Set.Transient<N> normalNodes;
    private final Set.Transient<N> artificialNodes;

    private final Map.Transient<Tuple2<TermIndex, String>, TransferFunctionAppl> tfAppls;
    private final Map.Transient<Tuple2<N, String>, ITerm> properties;
    private final BinaryRelation.Transient<N, N> directEdges;

    public ControlFlowGraph() {
        this.normalNodes = Set.Transient.of();
        this.startNodes = Set.Transient.of();
        this.endNodes = Set.Transient.of();
        this.artificialNodes = Set.Transient.of();

        this.tfAppls = Map.Transient.of();
        this.properties = Map.Transient.of();
        this.directEdges = BinaryRelation.Transient.of();
    }

    @Override
    public Set<N> getAllNodes() {
        return normalNodes.freeze().__insertAll(startNodes).__insertAll(endNodes);
    }

    @Override
    public Set<N> getStartNodes() {
        return startNodes;
    }

    @Override
    public Set<N> getEndNodes() {
        return endNodes;
    }

    @Override
    public Set<N> getNormalNodes() {
        return normalNodes;
    }

    @Override
    public Set<N> getArtificialNodes() {
        return artificialNodes;
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

    public void addTFAppl(N node, String prop, TransferFunctionAppl tfAppl) {
        tfAppls.__put(ImmutableTuple2.of(TermIndex.get(node).get(), prop), tfAppl);
    }

    public void addTFAppl(TermIndex index, String prop, TransferFunctionAppl tfAppl) {
        tfAppls.__put(ImmutableTuple2.of(index, prop), tfAppl);
    }

    public void setProperty(N node, String prop, ITerm value) {
        addNode(node);
        properties.__put(ImmutableTuple2.of(node, prop), value);
    }

    public TransferFunctionAppl getTFAppl(N node, String prop) {
        Optional<TransferFunctionAppl> tfApplOption = TermIndex.get(node).flatMap(index -> 
            Optional.ofNullable(tfAppls.get(ImmutableTuple2.of(index, prop))));
        if(tfApplOption.isPresent()) {
            return tfApplOption.get();
        }
        return new IdentityTFAppl<>(this, prop);
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
    
    protected void addNode(N node) {
        switch(node.getKind()) {
        case Artificial:
            artificialNodes.__insert(node);
            break;
        case End:
            endNodes.__insert(node);
            break;
        case Normal:
            normalNodes.__insert(node);
            break;
        case Start:
            startNodes.__insert(node);
            break;
        default:
            throw new RuntimeException("ICFGNode.Kind enum got another case that wasn't handled here");
        }
    }

    public void addDirectEdge(N sourceNode, N targetNode) {
        addNode(sourceNode);
        addNode(targetNode);
        directEdges.__insert(sourceNode, targetNode);
    }

    @Override
    public boolean isEmpty() {
        return directEdges.isEmpty();
    }

    public void addAll(IControlFlowGraph<N> controlFlowGraph) {
        this.startNodes.__insertAll(controlFlowGraph.getStartNodes());
        this.endNodes.__insertAll(controlFlowGraph.getEndNodes());
        this.normalNodes.__insertAll(controlFlowGraph.getNormalNodes());
        this.artificialNodes.__insertAll(controlFlowGraph.getArtificialNodes());
        
        this.tfAppls.__putAll(controlFlowGraph.getTFAppls());
        controlFlowGraph.getDirectEdges().entryIterator().forEachRemaining(entry -> {
            this.directEdges.__insert(entry.getKey(), entry.getValue());
        });
    }
    
    @Override
    public String toString() {
        return "ControlFlowGraph [startNodes=" + startNodes + ", endNodes=" + endNodes + ", normalNodes=" + normalNodes
                + ", artificialNodes=" + artificialNodes + ", tfAppls=" + tfAppls + ", properties=" + properties
                + ", directEdges=" + directEdges + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artificialNodes == null) ? 0 : artificialNodes.hashCode());
        result = prime * result + ((directEdges == null) ? 0 : directEdges.hashCode());
        result = prime * result + ((endNodes == null) ? 0 : endNodes.hashCode());
        result = prime * result + ((normalNodes == null) ? 0 : normalNodes.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((startNodes == null) ? 0 : startNodes.hashCode());
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
        if (artificialNodes == null) {
            if (other.artificialNodes != null)
                return false;
        } else if (!artificialNodes.equals(other.artificialNodes))
            return false;
        if (directEdges == null) {
            if (other.directEdges != null)
                return false;
        } else if (!directEdges.equals(other.directEdges))
            return false;
        if (endNodes == null) {
            if (other.endNodes != null)
                return false;
        } else if (!endNodes.equals(other.endNodes))
            return false;
        if (normalNodes == null) {
            if (other.normalNodes != null)
                return false;
        } else if (!normalNodes.equals(other.normalNodes))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (startNodes == null) {
            if (other.startNodes != null)
                return false;
        } else if (!startNodes.equals(other.startNodes))
            return false;
        if (tfAppls == null) {
            if (other.tfAppls != null)
                return false;
        } else if (!tfAppls.equals(other.tfAppls))
            return false;
        return true;
    }

    public static <T extends ICFGNode> ControlFlowGraph<T> of() {
        return new ControlFlowGraph<>();
    }

    /**
     * removes split/merge nodes
     */
    @Override
    public void complete() {
        for (N n : artificialNodes) {
            Set.Immutable<N> to = directEdges.get(n);
            Set.Immutable<N> from = directEdges.inverse().get(n);
            
            directEdges.__remove(n);
            for (N f : from) {
                directEdges.__remove(f, n);
                directEdges.__insert(f, to);
            }
        }
    }
}
