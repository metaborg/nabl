package mb.statix.concurrent.actors.deadlock;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.viatra.query.runtime.base.itc.alg.incscc.IncSCCAlg;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;

/**
 * A data structure to detect deadlock. Maintains a wait-for graph with tokens.
 */
public class WaitForGraph<N, S, T> {

    // FIXME Handling stopped nodes.
    //       a. resume of stopped node -- can be ignored.
    //       b. wait-for on stopped node -- could be resolved by an even later messages
    //       c. granted from a stopped node -- apply as usual
    //       d. suspend while having wait-fors on stopped nodes -- if the actor received all messages from the stopped node, it will be permanently stuck
    //       e. stopping -- if suspended actors have wait-fors on this node, and we didn't send them messages, they are permanently stuck

    private final LabeledGraph<N, T> waitForGraph = new LabeledGraph<>();
    private final IncSCCAlg<N> sccGraph = new IncSCCAlg<>(waitForGraph);
    private final Map.Transient<N, S> waitingNodes = Map.Transient.of();
    private final Set.Transient<N> stoppedNodes = Set.Transient.of();

    public WaitForGraph() {
    }

    /**
     * Register a wait-for in the graph.
     */
    public void waitFor(N source, T token, N target) {
        waitForGraph.addEdge(source, token, target);
    }

    /**
     * Remove a wait-for from the graph.
     */
    public void granted(N source, T token, N target) {
        waitForGraph.removeEdge(source, token, target);
    }

    /**
     * Node is activated.
     */
    public void resume(N node) {
        waitingNodes.__remove(node);
    }

    /**
     * Suspend a node. Return deadlocked tokens on the given node.
     */
    public Optional<Deadlock<N, S, T>> suspend(N node, S state) {
        waitingNodes.__put(node, state);
        return detectDeadlock(node);
    }

    /**
     * Remove a node, and any tokens this node was waiting on.
     */
    public void remove(N node) {
        waitingNodes.__remove(node);
        stoppedNodes.__insert(node);
        for(Entry<N, T> entry : waitForGraph.getOutgoingEdges(node).entrySet()) {
            final N target = entry.getKey();
            final T token = entry.getValue();
            waitForGraph.removeEdge(node, token, target);
        }
    }

    private Optional<Deadlock<N, S, T>> detectDeadlock(N node) {
        final N rep = sccGraph.getRepresentative(node);
        if(rep == null) {
            // node has no in- or outgoing edges
            return Optional.of(Deadlock.of(node, waitingNodes.get(node)));
        }
        if(sccGraph.hasOutgoingEdges(rep)) {
            // other clusters are upstream and may release tokens
            return Optional.empty();
        }
        final java.util.Set<N> scc =
                Optional.ofNullable(sccGraph.sccs.getPartition(rep)).orElse(Collections.singleton(node));
        if(!scc.stream().allMatch(waitingNodes::containsKey)) {
            // not all units are waiting yet
            return Optional.empty();
        }
        final Map.Transient<N, S> nodes = Map.Transient.of();
        for(N source : scc) {
            nodes.__put(source, waitingNodes.get(source));
        }
        final IRelation3.Transient<N, T, N> edges = HashTrieRelation3.Transient.of();
        for(N source : scc) {
            for(Entry<N, T> entry : waitForGraph.getOutgoingEdges(source).entrySet()) {
                final N target = entry.getKey();
                if(scc.contains(target)) {
                    final T token = entry.getValue();
                    edges.put(source, token, target);
                }
            }

        }
        return Optional.of(new Deadlock<>(nodes.freeze(), edges.freeze()));
    }

    @Override public String toString() {
        return waitForGraph.toString();
    }

}