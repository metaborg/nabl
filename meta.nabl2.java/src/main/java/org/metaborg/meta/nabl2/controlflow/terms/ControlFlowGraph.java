package org.metaborg.meta.nabl2.controlflow.terms;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.Tuple2;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;

public class ControlFlowGraph<N extends ICFGNode>
    implements IControlFlowGraph<N>, Serializable {

    private final Set.Transient<N> startNodes;
    private final Set.Transient<N> endNodes;
    private final Set.Transient<N> normalNodes;
    private final Set.Transient<N> artificialNodes;

    private final Map.Transient<Tuple2<TermIndex, String>, TransferFunctionAppl> tfAppls;
    private final Map.Transient<Tuple2<N, String>, ITerm> properties;
    private final BinaryRelation.Transient<N, N> directEdges;

    private Collection<java.util.Set<N>> topoSCCs;
    private Collection<java.util.Set<N>> revTopoSCCs;

    private List<N> unreachableNodes;

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
        Set.Transient<N> allNodes = Set.Transient.of();
        allNodes.__insertAll(normalNodes);
        allNodes.__insertAll(startNodes);
        allNodes.__insertAll(endNodes);
        return allNodes;
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

    @Override
    public Collection<java.util.Set<N>> getTopoSCCs() {
        return topoSCCs;
    }

    @Override
    public Collection<java.util.Set<N>> getRevTopoSCCs() {
        return revTopoSCCs;
    }

    @Override
    public List<N> getUnreachableNodes() {
        return unreachableNodes;
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
    public ITerm getProperty(N node, String prop) {
        return properties.get(ImmutableTuple2.of(node, prop));
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
        /* TODO: can we do better? SCCs are the same, topo order can be reversed, just the order within the
         * SCCs needs to be different. Perhaps faster to do ordering within SCCs as post processing?
         */
        computeTopoSCCs();
        computeRevTopoSCCs();
    }
    
    /**
     * The topologically sorted list of strongly connected components of the control-flow graph. In this
     * case we have start nodes for the graph so this is a slight adaptation where the nodes that are
     * unreachable from the start are returned as a separate list and are not analysed. We also sort the
     * SCCs internally to have a reverse postorder within the component. 
     * The basic algorithm is the SCC algorithm of Tarjan (1972), adapted so it doesn't give the _reverse_
     * topological sorted SCCs. 
     * @return A tuple of the topologically sorted list of strongly connected components and the unreachable nodes.
     */
    private void computeTopoSCCs() {
        Set<N> allNodes = this.getAllNodes();
        int index = 0;
        HashMap<N, Integer> nodeIndex = new HashMap<>(allNodes.size());
        for(N node : allNodes) {
            nodeIndex.put(node, Integer.MAX_VALUE);
        }
        HashMap<N, Integer> nodeLowlink = new HashMap<>(nodeIndex);
        Deque<N> sccStack = new ArrayDeque<>();
        java.util.Set<N> stackSet = new HashSet<>();
        Deque<java.util.Set<N>> sccs = new ArrayDeque<>();
        ArrayList<N> unreachable = new ArrayList<>();

        /* Note these deviations: 
         * (1) We seed the traversal with the start nodes.
         * (2) We use a deque of SCCs, so be can push to the front of it. 
         */
        for (N node : this.startNodes) {
            // For each start node that hasn't been visited already,
            if (nodeIndex.get(node) == Integer.MAX_VALUE) {
                // do the recursive strong-connect
                index = sccStrongConnect(node, index, nodeIndex, nodeLowlink, sccStack, stackSet, sccs, this.getDirectEdges());
            }
        }

        this.topoSCCs = Collections.unmodifiableCollection(sccs);

        for (N node : allNodes) {
            // Every node not yet visited from the start nodes is unreachable
            if (nodeIndex.get(node) == Integer.MAX_VALUE) {
                unreachable.add(node);
            }
        }

        this.unreachableNodes = Collections.unmodifiableList(unreachable);
    }
    private void computeRevTopoSCCs() {
        Set<N> allNodes = this.getAllNodes();
        int index = 0;
        HashMap<N, Integer> nodeIndex = new HashMap<>(allNodes.size());
        for(N node : allNodes) {
            nodeIndex.put(node, Integer.MAX_VALUE);
        }
        HashMap<N, Integer> nodeLowlink = new HashMap<>(nodeIndex);
        Deque<N> sccStack = new ArrayDeque<>();
        java.util.Set<N> stackSet = new HashSet<>();
        Deque<java.util.Set<N>> sccs = new ArrayDeque<>();

        /* Note these deviations: 
         * (1) We seed the traversal with the start nodes.
         * (2) We use a deque of SCCs, so be can push to the front of it. 
         */
        for (N node : this.endNodes) {
            // For each start node that hasn't been visited already,
            if (nodeIndex.get(node) == Integer.MAX_VALUE) {
                // do the recursive strong-connect
                index = sccStrongConnect(node, index, nodeIndex, nodeLowlink, sccStack, stackSet, sccs, this.getDirectEdges().inverse());
            }
        }

        this.revTopoSCCs = Collections.unmodifiableCollection(sccs);
    }

    /**
     * Recursively (DFS) walk the graph and give nodes an index. The lowlink is the lowest index of a node
     * that it can reach *through the DFS*. Therefore once those numbers are propagated, you can find an SCC
     * by finding all nodes with the same lowlink value. Given the way the algorithm works, when on the way
     * back from the DFS you find a node which still has the same index and lowlink value, this can be
     * considered the root of an SCC, and all the nodes above it on the stack are also in that SCC. So you
     * can simply pop nodes of the stack that was kept while doing the DFS (without inspecting them), until
     * you find this node with the same values.
     * As an adaption we also order the nodes in the SCC. We add visited nodes to the stack in post-order
     * even though the set of things on the stack is kept in pre-order. This allows us to very easily give
     * the nodes within an SCC in reverse-postorder
     * @param from The node to start from
     * @param index The index to start from
     * @param nodeIndex The mapping from node to index
     * @param nodeLowlink The mapping from node to lowest index reachable from this node
     * @param sccStack A stack of nodes being visited during the DFS (*not* used _for_ the DFS, it's recursive not iterative)
     * @param stackSet The set of nodes on the stack for easier checking if something's on the stack
     * @param sccs The list of SCCs
     * @return The new index value
     */
    private int sccStrongConnect(N from, int index, HashMap<N, Integer> nodeIndex, HashMap<N, Integer> nodeLowlink, 
            Deque<N> sccStack, java.util.Set<N> stackSet, Deque<java.util.Set<N>> sccs, BinaryRelation<N, N> edges) {
        nodeIndex.put(from, index);
        nodeLowlink.put(from, index);
        index++;

        // Note that we don't actually add the node to the stack, we just say it's on there with this set
        int stackSetSizeBefore = stackSet.size();
        stackSet.add(from);

        for (N to : edges.get(from)) {
            if (nodeIndex.get(to) == Integer.MAX_VALUE) {
                // Visit neighbours without an index. Propagate lowlink values backward. 
                index = this.sccStrongConnect(to, index, nodeIndex, nodeLowlink, sccStack, stackSet, sccs, edges);
                nodeLowlink.put(from, Integer.min(nodeLowlink.get(from), nodeLowlink.get(to)));
            } else if (stackSet.contains(to)) {
                /* Neighbours already in the stack are higher in the DFS spanning tree, so we use their
                 * index, not their lowlink. Using the lowlink doesn't break the algorithm, but doesn't help
                 * and makes the lowlink have a less predictable value which cannot be given a clear
                 * meaning.
                 */
                nodeLowlink.put(from, Integer.min(nodeLowlink.get(from), nodeIndex.get(to)));
            }
        }

        // Here we actually add the node to the stack, in _postorder_
        sccStack.add(from);

        if (nodeLowlink.get(from) == nodeIndex.get(from)) {
            // Pop the SCC of the stack; since it's a stack, we get a reverse postorder
            java.util.LinkedHashSet<N> scc = new LinkedHashSet<>();
            for(int i = stackSet.size(); i > stackSetSizeBefore; i--) {
                N node = sccStack.pop();
                stackSet.remove(node);
                scc.add(node);
            }
            // AddFirst so we get a topological ordering, not a reverse topological ordering
            sccs.addFirst(Collections.unmodifiableSet(scc));
        }

        return index;
    }
}
