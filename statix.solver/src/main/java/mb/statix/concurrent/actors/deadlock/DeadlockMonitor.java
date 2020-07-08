package mb.statix.concurrent.actors.deadlock;

import java.util.Map;
import java.util.Map.Entry;

import org.metaborg.util.functions.Action2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Maps;

import mb.nabl2.util.collections.IRelation3;
import mb.nabl2.util.collections.IRelation3.Immutable;
import mb.nabl2.util.collections.MultiSet;
import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;

public class DeadlockMonitor<T> implements IDeadlockMonitor<T> {

    private static final ILogger logger = LoggerUtils.logger(DeadlockMonitor.class);

    private final IActor<? extends IDeadlockMonitor<T>> self;

    private final WaitForGraph<IActorRef<?>, T> wfg = new WaitForGraph<>();
    private final Map<IActorRef<?>, MultiSet.Immutable<IActorRef<?>>> sent = Maps.newHashMap(); // per actor, messages sent to it by others
    private Action2<IActor<?>, Immutable<IActorRef<?>, T, IActorRef<?>>> handler;

    public DeadlockMonitor(IActor<? extends IDeadlockMonitor<T>> self,
            Action2<IActor<?>, IRelation3.Immutable<IActorRef<?>, T, IActorRef<?>>> handler) {
        this.self = self;
        this.handler = handler;
    }

    @Override public void waitFor(IActorRef<?> actor, T token) {
        logger.info("dlm {} waitFor {} / {}", self.sender(), actor, token);
        wfg.waitFor(self.sender(), token, actor);
    }

    @Override public void granted(IActorRef<?> actor, T token) {
        logger.info("dlm {} granted {} / {}", self.sender(), actor, token);
        wfg.granted(self.sender(), token, actor);
    }

    @Override public void suspended(Clock clock) {
        if(!processClock(self.sender(), clock)) {
            return;
        }
        wfg.suspend(self.sender()).ifPresent(waitFors -> {
            logger.info("{} deadlocked: {}", self.sender(), waitFors);
            logger.info("wfg: {}", wfg);
            handler.apply(self, waitFors);
        });
    }

    @Override public void stopped(Clock clock) {
        if(!processClock(self.sender(), clock)) {
            return;
        }
        wfg.remove(self.sender());
        logger.warn("{} stopped", self.sender());
    }

    /**
     * Process the clock of a received event. This activates any suspended actors that have received messages from the
     * given actor since their last event, and updates their clocks to the latest known number of sent messages. Returns
     * whether this actor received at least all messages that we know about.
     */
    private boolean processClock(final IActorRef<?> current, final Clock clock) {
        // process sent messages, and resume receiving actors
        for(Entry<IActorRef<?>, Integer> entry : clock.sent().entrySet()) {
            final IActorRef<?> receiver = entry.getKey();
            final int sent = entry.getValue();
            final MultiSet.Immutable<IActorRef<?>> receiverClock =
                    this.sent.computeIfAbsent(receiver, __ -> MultiSet.Immutable.of());
            if(receiverClock.count(current) < sent) {
                this.sent.put(receiver, receiverClock.set(current, sent));
                wfg.resume(receiver);
            }
        }

        // check if any actor sent us messages we haven't received
        boolean atleast = true;
        final MultiSet.Transient<IActorRef<?>> receivedClock = clock.received().melt();
        for(Entry<IActorRef<?>, Integer> entry : this.sent.computeIfAbsent(current, __ -> MultiSet.Immutable.of())
                .entrySet()) {
            final IActorRef<?> sender = entry.getKey();
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