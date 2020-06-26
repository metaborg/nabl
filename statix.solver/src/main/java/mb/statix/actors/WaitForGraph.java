package mb.statix.actors;

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
 * A thread-safe data structure to detect deadlock. Maintains a wait-for graph to do this.
 */
public class WaitForGraph<N, T> {

    private final Graph<N> requestGraph = new Graph<>();
    private final IncSCCAlg<N> sccGraph = new IncSCCAlg<>(requestGraph);
    private final Set<N> waiting = new HashSet<>();
    private final IRelation3.Transient<N, T, N> waitFor = HashTrieRelation3.Transient.of();

    private final Object lock = new Object();

    public WaitForGraph() {
    }

    /**
     * Register a wait-for in the graph.
     */
    public void waitFor(N source, T token, N target) {
        synchronized(lock) {
            if(waiting.contains(source)) {
                throw new IllegalStateException("Node " + source + " is not active.");
            }
            requestGraph.insertNode(source);
            requestGraph.insertNode(target);
            requestGraph.insertEdge(source, target);
            // ASSERT is not already in the graph
            waitFor.put(source, token, target);
        }
    }

    /**
     * Remove a wait-for from the graph.
     */
    public void granted(N source, T token, N target) {
        synchronized(lock) {
            if(waiting.contains(source)) {
                throw new IllegalStateException("Node " + source + " is not active.");
            }
            requestGraph.deleteEdgeThatExists(source, target);
            waitFor.remove(source, token, target);
        }
    }

    /**
     * Node is activated.
     */
    public void activate(N node) {
        synchronized(lock) {
            if(!waiting.contains(node)) {
                throw new IllegalStateException("Node " + node + " is not waiting.");
            }
            waiting.remove(node);
        }
    }

    /**
     * Suspend a node. Return deadlocked tokens on the given node.
     */
    public SetMultimap<N, T> suspend(N node) {
        synchronized(lock) {
            if(waiting.contains(node)) {
                throw new IllegalStateException("Node " + node + " is already waiting.");
            }
            requestGraph.insertNode(node);
            waiting.add(node);
            return detectDeadlock(node);
        }
    }

    /**
     * Remove a node. Return tokens waiting on that node.
     */
    public SetMultimap<N, T> remove(N node) {
        synchronized(lock) {
            requestGraph.deleteNode(node);
            waiting.remove(node);
            final SetMultimap.Transient<N, T> waits = SetMultimap.Transient.of();
            for(Entry<T, N> entry : waitFor.inverse().get(node)) {
                final N other = entry.getValue();
                final T token = entry.getKey();
                waitFor.remove(other, token, node);
                waits.__insert(other, token);
            }
            return waits.freeze();
        }
    }

    private SetMultimap<N, T> detectDeadlock(N node) {
        final N rep = sccGraph.getRepresentative(node);
        if(sccGraph.hasOutgoingEdges(rep)) {
            // other clusters are upstream and may release tokens
            return SetMultimap.Immutable.of();
        }
        final Set<N> scc = Optional.ofNullable(sccGraph.sccs.getPartition(node)).orElse(Collections.singleton(node));
        if(!scc.stream().allMatch(waiting::contains)) {
            // some units are active
            return SetMultimap.Immutable.of();
        }
        final SetMultimap.Transient<N, T> waits = SetMultimap.Transient.of();
        for(Entry<T, N> entry : waitFor.inverse().get(node)) {
            final N other = entry.getValue();
            if(scc.contains(other)) {
                final T token = entry.getKey();
                waitFor.remove(other, token, node);
                waits.__insert(other, token);
            }
        }
        return waits.freeze();
    }

}