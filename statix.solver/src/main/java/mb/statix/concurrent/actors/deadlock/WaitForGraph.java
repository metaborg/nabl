package mb.statix.concurrent.actors.deadlock;

import java.util.Collections;
import java.util.Optional;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.Map;
import mb.nabl2.util.graph.alg.incscc.IncSCCAlg;
import mb.nabl2.util.graph.graphimpl.Graph;

/**
 * A data structure to detect deadlock. Maintains a wait-for graph with tokens.
 */
public class WaitForGraph<N> {

    private static final ILogger logger = LoggerUtils.logger(WaitForGraph.class);

    private final Graph<N> waitForGraph = new Graph<>();
    private final IncSCCAlg<N> sccGraph = new IncSCCAlg<>(waitForGraph);

    private final LogicalClockSet<N> waitingNodes = new LogicalClockSet<>();

    public WaitForGraph() {
    }

    public boolean isWaiting(N node) {
        return waitingNodes.contains(node);
    }

    /**
     * Register a wait-for in the graph.
     */
    public void waitFor(N source, N target) {
        logger.debug("{} waits for {}", source, target);
        waitForGraph.insertNode(source);
        waitForGraph.insertNode(target);
        waitForGraph.insertEdge(source, target);
    }

    /**
     * Remove a wait-for from the graph.
     */
    public void granted(N source, N target) {
        logger.debug("{} was granted {}", source, target);
        waitForGraph.deleteEdgeThatExists(source, target);
    }

    /**
     * Suspend a node. Return deadlocked tokens on the given node.
     */
    public Deadlock<N> suspend(N node, Clock<N> clock) {
        logger.debug("{} suspended {}", node, clock);
        if(!waitingNodes.add(node, clock)) {
            return Deadlock.empty();
        }
        return detectDeadlock(node);
    }

    private Deadlock<N> detectDeadlock(N node) {
        final N rep = sccGraph.getRepresentative(node);
        if(rep == null) {
            // node has no in- or outgoing edges
            return Deadlock.empty();
        }
        if(sccGraph.hasOutgoingEdges(rep)) {
            // other clusters are upstream and may release tokens
            return Deadlock.empty();
        }
        final java.util.Set<N> scc =
                Optional.ofNullable(sccGraph.sccs.getPartition(rep)).orElse(Collections.singleton(node));
        if(!scc.stream().allMatch(waitingNodes::contains)) {
            // not all units are waiting yet
            return Deadlock.empty();
        }
        boolean hasEdges = false;
        for(N source : scc) {
            for(N target : waitForGraph.getTargetNodes(source)) {
                if(scc.contains(target)) {
                    hasEdges = true;
                }
            }

        }
        final Map.Transient<N, Clock<N>> nodes = Map.Transient.of();
        if(hasEdges) {
            for(N source : scc) {
                nodes.__put(source, waitingNodes.get(source));
            }
        }
        return new Deadlock<>(nodes.freeze());
    }

    @Override public String toString() {
        return waitForGraph.toString();
    }

}
