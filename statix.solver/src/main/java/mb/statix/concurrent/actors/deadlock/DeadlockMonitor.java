package mb.statix.concurrent.actors.deadlock;

import java.util.Map;
import java.util.Map.Entry;

import org.metaborg.util.functions.Action2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Maps;

import mb.nabl2.util.collections.MultiSet;
import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;

public class DeadlockMonitor<N, S, T> implements IDeadlockMonitor<N, S, T> {

    private static final ILogger logger = LoggerUtils.logger(DeadlockMonitor.class);

    private final IActor<? extends IDeadlockMonitor<N, S, T>> self;

    private final WaitForGraph<IActorRef<? extends N>, S, T> wfg = new WaitForGraph<>();
    private final Map<IActorRef<? extends N>, Clock<N>> clocks = Maps.newHashMap();
    private final Map<IActorRef<? extends N>, MultiSet.Immutable<IActorRef<? extends N>>> sent = Maps.newHashMap(); // per actor, messages sent to it by others
    private Action2<IActor<?>, Deadlock<IActorRef<? extends N>, S, T>> handler;

    public DeadlockMonitor(IActor<? extends IDeadlockMonitor<N, S, T>> self,
            Action2<IActor<?>, Deadlock<IActorRef<? extends N>, S, T>> handler) {
        this.self = self;
        this.handler = handler;
    }

    @Override public void waitFor(IActorRef<? extends N> actor, T token) {
        logger.info("dlm {} waitFor {} / {}", sender(), actor, token);
        wfg.waitFor(sender(), token, actor);
    }

    @Override public void granted(IActorRef<? extends N> actor, T token) {
        logger.info("dlm {} granted {} / {}", sender(), actor, token);
        wfg.granted(sender(), token, actor);
    }

    @Override public void suspended(S state, Clock<N> clock) {
        if(!processClock(sender(), clock)) {
            return;
        }
        wfg.suspend(sender(), state).ifPresent(deadlock -> {
            logger.info("{} deadlocked: {}", sender(), deadlock);
            logger.info("wfg: {}", wfg);
            handler.apply(self, deadlock);
        });
    }

    @Override public void stopped(Clock<N> clock) {
        if(!processClock(sender(), clock)) {
            return;
        }
        wfg.remove(sender());
        logger.warn("{} stopped", self.sender());
    }

    @SuppressWarnings({ "unchecked" }) private IActorRef<? extends N> sender() {
        return (IActorRef<? extends N>) self.sender();
    }

    /**
     * Process the clock of a received event. This activates any suspended actors that have received messages from the
     * given actor since their last event, and updates their clocks to the latest known number of sent messages. Returns
     * whether this actor received at least all messages that we know about.
     */
    private boolean processClock(final IActorRef<? extends N> current, final Clock<N> clock) {
        if(clocks.computeIfAbsent(current, __ -> Clock.of()).equals(clock)) {
            return false;
        }
        clocks.put(current, clock);

        // process sent messages, and resume receiving actors
        for(Entry<IActorRef<? extends N>, Integer> entry : clock.sent().entrySet()) {
            final IActorRef<? extends N> receiver = entry.getKey();
            final int sent = entry.getValue();
            final MultiSet.Immutable<IActorRef<? extends N>> receiverClock =
                    this.sent.computeIfAbsent(receiver, __ -> MultiSet.Immutable.of());
            if(receiverClock.count(current) < sent) {
                this.sent.put(receiver, receiverClock.set(current, sent));
                wfg.resume(receiver);
            }
        }

        // check if any actor sent us messages we haven't received
        boolean atleast = true;
        final MultiSet.Transient<IActorRef<? extends N>> receivedClock = clock.received().melt();
        for(Entry<IActorRef<? extends N>, Integer> entry : this.sent
                .computeIfAbsent(current, __ -> MultiSet.Immutable.of()).entrySet()) {
            final IActorRef<? extends N> sender = entry.getKey();
            final int sent = entry.getValue();
            if(receivedClock.count(sender) < sent) {
                receivedClock.set(sender, sent);
                atleast = false;
            }
        }
        this.sent.put(current, receivedClock.freeze());

        return atleast;
    }

}