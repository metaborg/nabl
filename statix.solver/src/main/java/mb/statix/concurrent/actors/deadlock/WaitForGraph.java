package mb.statix.concurrent.actors.deadlock;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Optional;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSetMap;
import mb.nabl2.util.graph.alg.incscc.IncSCCAlg;

/**
 * A data structure to detect deadlock. Maintains a wait-for graph with tokens.
 */
public class WaitForGraph<N, T> {

    private static final ILogger logger = LoggerUtils.logger(WaitForGraph.class);

    private final LabeledGraph<N, T> waitForGraph = new LabeledGraph<>();
    private final IncSCCAlg<N> sccGraph = new IncSCCAlg<>(waitForGraph);

    private final Map.Transient<N, Clock<N>> clocks = Map.Transient.of();
    private final Map.Transient<N, MultiSet.Immutable<N>> knownMessagesTo = Map.Transient.of(); // per actor, messages sent to it by others

    private final Set.Transient<N> waitingNodes = Set.Transient.of();

    public WaitForGraph() {
    }

    public boolean isWaiting(N node) {
        return waitingNodes.contains(node);
    }

    /**
     * Register a wait-for in the graph.
     */
    public void waitFor(N source, T token, N target) {
        logger.debug("{} waits for {}/{}", source, target, token);
        waitForGraph.addEdge(source, token, target);
    }

    /**
     * Remove a wait-for from the graph.
     */
    public void granted(N source, T token, N target) {
        logger.debug("{} was granted {}/{}", source, target, token);
        waitForGraph.removeEdge(source, token, target);
    }

    /**
     * Suspend a node. Return deadlocked tokens on the given node.
     */
    public Deadlock<N, T> suspend(N node, Clock<N> clock) {
        logger.debug("{} suspended {}", node, clock);
        if(!processClock(node, clock)) {
            return Deadlock.empty();
        }
        waitingNodes.__insert(node);
        return detectDeadlock(node);
    }

    /**
     * Process the clock of a received event. This activates any suspended actors that have received messages from the
     * given actor since their last event, and updates their clocks to the latest known number of sent messages. Returns
     * a boolean indicating whether the node processed all messages we know it has been sent.
     */
    private boolean processClock(final N node, final Clock<N> clock) {
        if(clocks.containsKey(node) && clocks.get(node).equals(clock)) {
            logger.debug("{} stale event {}", node, clock);
            // It is important to ignore stale events, as suspends may happen even
            // if no progress is made, e.g., as a result of processing an earlier
            // deadlock messages. Not ignoring it could lead to non-termination, if
            // empty deadlocks are also reported to the unit that suspends.
            return false;
        }
        clocks.__put(node, clock);

        // process sent messages, and resume receiving actors
        for(Entry<N, Integer> entry : clock.sent().entrySet()) {
            final N receiver = entry.getKey();
            final int sent = entry.getValue();
            final MultiSet.Immutable<N> receiverClock =
                    this.knownMessagesTo.getOrDefault(receiver, MultiSet.Immutable.of());
            final int received = receiverClock.count(node);
            if(sent > received) {
                this.knownMessagesTo.put(receiver, receiverClock.set(node, sent));
                if(receiver.equals(node)) {
                    // Do not activate the node this clock belongs to, but leave that to the next part of this method.
                    // If we activate the node here, it may be activated if it sent itself a message that it already received, since
                    // the received count is not yet updated here.
                    continue;
                }
                if(waitingNodes.__remove(receiver)) {
                    logger.debug("{} activates {} (sent {} > received {})", node, receiver, sent, received);
                }
            }
        }

        // check if any actor sent us messages we haven't received
        boolean atleast = true;
        final MultiSet.Transient<N> receivedClock = clock.delivered().melt();
        for(Entry<N, Integer> entry : this.knownMessagesTo.getOrDefault(node, MultiSet.Immutable.of()).entrySet()) {
            final N sender = entry.getKey();
            final int sent = entry.getValue();
            final int received = receivedClock.count(sender);
            if(received < sent) {
                logger.debug("{} kept active by {} (received {} < sent {})", node, sender, received, sent);
                receivedClock.set(sender, sent);
                atleast = false;
            }
        }
        this.knownMessagesTo.put(node, receivedClock.freeze());

        return atleast;
    }

    private Deadlock<N, T> detectDeadlock(N node) {
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
        final Map.Transient<N, Clock<N>> nodes = Map.Transient.of();
        final MultiSetMap.Transient<Tuple2<N, N>, T> edges = MultiSetMap.Transient.of();
        for(N source : scc) {
            for(Entry<N, MultiSet.Immutable<T>> entry : waitForGraph.getOutgoingEdges(source).toMap().entrySet()) {
                final N target = entry.getKey();
                if(scc.contains(target)) {
                    final Tuple2<N, N> key = Tuple2.of(source, target);
                    edges.putAll(key, entry.getValue());
                }
            }

        }
        if(!edges.isEmpty()) {
            for(N source : scc) {
                nodes.__put(source, clocks.get(source));
            }
        }
        return new Deadlock<>(nodes.freeze(), edges.freeze());
    }

    @Override public String toString() {
        return waitForGraph.toString();
    }

}
