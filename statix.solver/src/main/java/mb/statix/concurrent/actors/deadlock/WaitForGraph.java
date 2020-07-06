package mb.statix.concurrent.actors.deadlock;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.eclipse.viatra.query.runtime.base.itc.alg.incscc.IncSCCAlg;
import org.eclipse.viatra.query.runtime.base.itc.graphimpl.Graph;

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

    private final Graph<N> waitForGraph = new Graph<>();
    private final IncSCCAlg<N> sccGraph = new IncSCCAlg<>(waitForGraph);
    private final Set<N> waitingNodes = new HashSet<>();
    private final Set<N> stoppedNodes = new HashSet<>();
    private final IRelation3.Transient<N, T, N> waitForEdges = HashTrieRelation3.Transient.of();

    public WaitForGraph() {
    }

    private void addNodeIfAbsent(N node) {
        if(!waitForGraph.getAllNodes().contains(node)) {
            waitForGraph.insertNode(node);
        }
    }

    /**
     * Register a wait-for in the graph.
     */
    public void waitFor(N source, T token, N target) {
        addNodeIfAbsent(source);
        addNodeIfAbsent(target);

        waitForGraph.insertEdge(source, target);
        // ASSERT is not already in the graph
        waitForEdges.put(source, token, target);
    }

    /**
     * Remove a wait-for from the graph.
     */
    public void granted(N source, T token, N target) {
        waitForGraph.deleteEdgeThatExists(source, target);
        waitForEdges.remove(source, token, target);
    }

    /**
     * Node is activated.
     */
    public void resume(N node) {
        addNodeIfAbsent(node);

        waitingNodes.remove(node);
    }

    /**
     * Suspend a node. Return deadlocked tokens on the given node.
     */
    public Optional<IRelation3.Immutable<N, T, N>> suspend(N node) {
        addNodeIfAbsent(node);

        waitingNodes.add(node);
        return detectDeadlock(node);
    }

    /**
     * Remove a node. Return tokens the node was waiting on.
     */
    public IRelation3.Immutable<N, T, N> remove(N node) {
        addNodeIfAbsent(node);

        waitForGraph.deleteNode(node);
        waitingNodes.remove(node);
        stoppedNodes.add(node);
        final IRelation3.Transient<N, T, N> waitFors = HashTrieRelation3.Transient.of();
        for(Entry<T, N> entry : waitForEdges.get(node)) {
            final N target = entry.getValue();
            final T token = entry.getKey();
            waitForEdges.remove(node, token, target);
            waitFors.put(node, token, target);
        }
        return waitFors.freeze();
    }

    private Optional<IRelation3.Immutable<N, T, N>> detectDeadlock(N node) {
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
        final IRelation3.Transient<N, T, N> waitFors = HashTrieRelation3.Transient.of();
        for(N source : scc) {
            for(Entry<T, N> entry : waitForEdges.get(source)) {
                final N target = entry.getValue();
                if(scc.contains(target)) {
                    final T token = entry.getKey();
                    waitForEdges.remove(source, token, target);
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