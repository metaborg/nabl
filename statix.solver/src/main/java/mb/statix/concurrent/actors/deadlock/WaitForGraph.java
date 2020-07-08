package mb.statix.concurrent.actors.deadlock;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.eclipse.viatra.query.runtime.base.itc.alg.incscc.IncSCCAlg;

import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;

/**
 * A data structure to detect deadlock. Maintains a wait-for graph with tokens.
 */
public class WaitForGraph<N, T> {

    // FIXME Handling stopped nodes.
    //       a. resume of stopped node -- can be ignored.
    //       b. wait-for on stopped node -- could be resolved by an even later messages
    //       c. granted from a stopped node -- apply as usual
    //       d. suspend while having wait-fors on stopped nodes -- if the actor received all messages from the stopped node, it will be permanently stuck
    //       e. stopping -- if suspended actors have wait-fors on this node, and we didn't send them messages, they are permanently stuck

    private final LabeledGraph<N, T> waitForGraph = new LabeledGraph<>();
    private final IncSCCAlg<N> sccGraph = new IncSCCAlg<>(waitForGraph);
    private final Set<N> waitingNodes = new HashSet<>();
    private final Set<N> stoppedNodes = new HashSet<>();

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
        waitingNodes.remove(node);
    }

    /**
     * Suspend a node. Return deadlocked tokens on the given node.
     */
    public Optional<IRelation3.Immutable<N, T, N>> suspend(N node) {
        waitingNodes.add(node);
        return detectDeadlock(node);
    }

    /**
     * Remove a node, and any tokens this node was waiting on.
     */
    public void remove(N node) {
        waitingNodes.remove(node);
        stoppedNodes.add(node);
        for(Entry<N, T> entry : waitForGraph.getOutgoingEdges(node).entrySet()) {
            final N target = entry.getKey();
            final T token = entry.getValue();
            waitForGraph.removeEdge(node, token, target);
        }
    }

    private Optional<IRelation3.Immutable<N, T, N>> detectDeadlock(N node) {
        final N rep = sccGraph.getRepresentative(node);
        if(rep == null) {
            // node has no in- or outgoing edges
            return Optional.of(HashTrieRelation3.Immutable.of());
        }
        if(sccGraph.hasOutgoingEdges(rep)) {
            // other clusters are upstream and may release tokens
            return Optional.empty();
        }
        final Set<N> scc = Optional.ofNullable(sccGraph.sccs.getPartition(rep)).orElse(Collections.singleton(node));
        if(!scc.stream().allMatch(waitingNodes::contains)) {
            // not all units are waiting yet
            return Optional.empty();
        }
        final IRelation3.Transient<N, T, N> waitFors = HashTrieRelation3.Transient.of();
        for(N source : scc) {
            for(Entry<N, T> entry : waitForGraph.getOutgoingEdges(source).entrySet()) {
                final N target = entry.getKey();
                if(scc.contains(target)) {
                    final T token = entry.getValue();
                    waitFors.put(source, token, target);
                }
            }

        }
        return Optional.of(waitFors.freeze());
    }

    @Override public String toString() {
        return waitForGraph.toString();
    }

}