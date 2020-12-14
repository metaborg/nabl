package mb.statix.concurrent.actors.deadlock;

import java.util.Map.Entry;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.MultiSet;

/**
 * Set which maintains a set of communicating nodes. Nodes are only part of the set if they are explicitly added, and no
 * unreceived messages to that node from other nodes are known of. The set is initially empty.
 */
public class LogicalClockSet<N> {

    private static final ILogger logger = LoggerUtils.logger(LogicalClockSet.class);

    private final Map.Transient<N, Clock<N>> clocks = Map.Transient.of();
    private final Map.Transient<N, MultiSet.Immutable<N>> knownMessagesTo = Map.Transient.of(); // per actor, messages sent to it by others

    private final Set.Transient<N> waitingNodes = Set.Transient.of();

    public boolean contains(final N node) {
        return waitingNodes.contains(node);
    }

    public Clock<N> get(final N node) {
        return clocks.get(node);
    }

    /**
     * Add a node with its clock. This removes any nodes that have received messages from the given node since their
     * last addition, and updates their clocks to the latest known number of sent messages. Returns a boolean indicating
     * whether the node processed all messages we know it has been sent.
     */
    public boolean add(final N node, final Clock<N> clock) {
        if(clocks.containsKey(node)) {
            final Clock<N> oldClock = clocks.get(node);
            Integer d;
            if((d = oldClock.compareTo(clock).orElse(null)) == null) {
                throw new IllegalArgumentException("Clocks must be monotone.");
            } else if(d >= 0) {
                logger.debug("{} stale event {}", node, clock);
                return false;
            }
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
                    logger.debug("{} removed {} (sent {} > received {})", node, receiver, sent, received);
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
                logger.debug("{} kept by {} (received {} < sent {})", node, sender, received, sent);
                receivedClock.set(sender, sent);
                atleast = false;
            }
        }
        this.knownMessagesTo.put(node, receivedClock.freeze());

        if(atleast) {
            logger.debug("{} added", node);
            waitingNodes.__insert(node);
        }
        return atleast;
    }

}