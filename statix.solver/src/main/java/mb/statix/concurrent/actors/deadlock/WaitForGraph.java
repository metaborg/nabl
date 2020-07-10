package mb.statix.concurrent.actors.deadlock;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.viatra.query.runtime.base.itc.alg.incscc.IncSCCAlg;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSetMap;

/**
 * A data structure to detect deadlock. Maintains a wait-for graph with tokens.
 */
public class WaitForGraph<N, S, T> {

    private static final ILogger logger = LoggerUtils.logger(WaitForGraph.class);

    // FIXME Handling stopped nodes.
    //       a. resume of stopped node -- can be ignored.
    //       b. wait-for on stopped node -- could be resolved by an even later messages
    //       c. granted from a stopped node -- apply as usual
    //       d. suspend while having wait-fors on stopped nodes -- if the actor received all messages from the stopped node, it will be permanently stuck
    //       e. stopping -- if suspended actors have wait-fors on this node, and we didn't send them messages, they are permanently stuck

    private final LabeledGraph<N, T> waitForGraph = new LabeledGraph<>();
    private final IncSCCAlg<N> sccGraph = new IncSCCAlg<>(waitForGraph);

    private final Map.Transient<N, Clock<N>> clocks = Map.Transient.of();
    private final Map.Transient<N, MultiSet.Immutable<N>> knownMessagesTo = Map.Transient.of(); // per actor, messages sent to it by others

    private final Map.Transient<N, S> waitingNodes = Map.Transient.of();
    private final Set.Transient<N> stoppedNodes = Set.Transient.of();

    public WaitForGraph() {
    }

    public boolean isWaiting(N node) {
        return waitingNodes.containsKey(node);
    }

    /**
     * Register a wait-for in the graph.
     */
    public void waitFor(N source, T token, N target) {
        logger.info("{} waits for {}/{}", source, target, token);
        waitForGraph.addEdge(source, token, target);
    }

    /**
     * Remove a wait-for from the graph.
     */
    public void granted(N source, T token, N target) {
        logger.info("{} was granted {}/{}", source, target, token);
        waitForGraph.removeEdge(source, token, target);
    }

    /**
     * Suspend a node. Return deadlocked tokens on the given node.
     */
    public Optional<Optional<Deadlock<N, S, T>>> suspend(N node, S state, Clock<N> clock) {
        logger.info("{}[{}] suspended {}", node, state, clock);
        if(!processClock(node, clock)) {
            return Optional.empty();
        }
        waitingNodes.__put(node, state);
        return Optional.of(detectDeadlock(node));
    }

    /**
     * Remove a node, and any tokens this node was waiting on.
     */
    public void remove(N node, Clock<N> clock) {
        logger.info("{} stopped {}", node, clock);
        processClock(node, clock);
        waitingNodes.__remove(node);
        stoppedNodes.__insert(node);
        for(Entry<N, MultiSet.Immutable<T>> entry : waitForGraph.getOutgoingEdges(node).toMap().entrySet()) {
            final N target = entry.getKey();
            for(T token : entry.getValue()) {
                waitForGraph.removeEdge(node, token, target);
            }
        }
    }

    /**
     * Process the clock of a received event. This activates any suspended actors that have received messages from the
     * given actor since their last event, and updates their clocks to the latest known number of sent messages. Returns
     * an integer indicating whether all messages were processed: negative means some messages were not yet processed,
     * zero means nothing changed, and positive means it processed unknown messages.
     */
    private boolean processClock(final N node, final Clock<N> clock) {
        if(clocks.containsKey(node) && clocks.get(node).equals(clock)) {
            logger.info("{} stale event {}", node, clock);
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
                waitingNodes.__remove(receiver);
                logger.info("{} activates {} (sent {} > received {})", node, receiver, sent, received);
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
                logger.info("{} kept active by {} (received {} < sent {})", node, sender, received, sent);
                receivedClock.set(sender, sent);
                atleast = false;
            }
        }
        this.knownMessagesTo.put(node, receivedClock.freeze());

        return atleast;
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
        return Optional.of(new Deadlock<>(nodes.freeze(), edges.freeze()));
    }

    @Override public String toString() {
        return waitForGraph.toString();
    }

}