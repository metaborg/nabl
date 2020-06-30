package mb.statix.actors.deadlock;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.eclipse.viatra.query.runtime.base.itc.alg.incscc.IncSCCAlg;
import org.eclipse.viatra.query.runtime.base.itc.graphimpl.Graph;

import io.usethesource.capsule.SetMultimap;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;

/**
 * A data structure to detect deadlock. Maintains a wait-for graph with tokens.
 */
public class WaitForGraph<N, T> {

    private final Graph<N> waitForGraph = new Graph<>();
    private final IncSCCAlg<N> sccGraph = new IncSCCAlg<>(waitForGraph);
    private final Set<N> waitingNodes = new HashSet<>();
    private final IRelation3.Transient<N, T, N> waitForEdges = HashTrieRelation3.Transient.of();

    public WaitForGraph() {
    }

    public void add(N node) {
        waitForGraph.insertNode(node);
    }

    /**
     * Register a wait-for in the graph.
     */
    public void waitFor(N source, T token, N target) {
        if(waitingNodes.contains(source)) {
            throw new IllegalStateException("Node " + source + " is not active.");
        }
        waitForGraph.insertEdge(source, target);
        // ASSERT is not already in the graph
        waitForEdges.put(source, token, target);
    }

    /**
     * Remove a wait-for from the graph.
     */
    public void granted(N source, T token, N target) {
        if(waitingNodes.contains(source)) {
            throw new IllegalStateException("Node " + source + " is not active.");
        }
        waitForGraph.deleteEdgeThatExists(source, target);
        waitForEdges.remove(source, token, target);
    }

    /**
     * Node is activated.
     */
    public void activate(N node) {
        if(!waitingNodes.contains(node)) {
            throw new IllegalStateException("Node " + node + " is not waiting.");
        }
        waitingNodes.remove(node);
    }

    /**
     * Suspend a node. Return deadlocked tokens on the given node.
     */
    public Optional<SetMultimap<N, T>> suspend(N node) {
        if(waitingNodes.contains(node)) {
            throw new IllegalStateException("Node " + node + " is already waiting.");
        }
        waitingNodes.add(node);
        return detectDeadlock(node);
    }

    /**
     * Remove a node. Return tokens waiting on that node.
     */
    public SetMultimap<N, T> remove(N node) {
        waitForGraph.deleteNode(node);
        waitingNodes.remove(node);
        final SetMultimap.Transient<N, T> waits = SetMultimap.Transient.of();
        for(Entry<T, N> entry : waitForEdges.inverse().get(node)) {
            final N other = entry.getValue();
            final T token = entry.getKey();
            waitForEdges.remove(other, token, node);
            waits.__insert(other, token);
        }
        return waits.freeze();
    }

    private Optional<SetMultimap<N, T>> detectDeadlock(N node) {
        final N rep = sccGraph.getRepresentative(node);
        if(sccGraph.hasOutgoingEdges(rep)) {
            // other clusters are upstream and may release tokens
            return Optional.empty();
        }
        final Set<N> scc = Optional.ofNullable(sccGraph.sccs.getPartition(node)).orElse(Collections.singleton(node));
        if(!scc.stream().allMatch(waitingNodes::contains)) {
            // not all units are waiting yet
            return Optional.empty();
        }
        final SetMultimap.Transient<N, T> waits = SetMultimap.Transient.of();
        for(Entry<T, N> entry : waitForEdges.inverse().get(node)) {
            final N other = entry.getValue();
            if(scc.contains(other)) {
                final T token = entry.getKey();
                waitForEdges.remove(other, token, node);
                waits.__insert(other, token);
            }
        }
        return Optional.of(waits.freeze());
    }

    @Override public String toString() {
        return waitForGraph.toString();
    }

}