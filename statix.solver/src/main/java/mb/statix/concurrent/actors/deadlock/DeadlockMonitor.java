package mb.statix.concurrent.actors.deadlock;

import java.util.Map.Entry;

import org.metaborg.util.functions.Action2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSetMap;
import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;

public class DeadlockMonitor<N, S, T> implements IDeadlockMonitor<N, T> {

    private static final ILogger logger = LoggerUtils.logger(DeadlockMonitor.class);

    private final TypeTag<? extends N> TYPE = TypeTag.of(Object.class);

    private final IActor<? extends IDeadlockMonitor<N, T>> self;

    private final WaitForGraph<IActorRef<? extends N>, T> wfg = new WaitForGraph<>();
    private Action2<IActor<?>, Deadlock<IActorRef<? extends N>, T>> handler;

    public DeadlockMonitor(IActor<? extends IDeadlockMonitor<N, T>> self,
            Action2<IActor<?>, Deadlock<IActorRef<? extends N>, T>> handler) {
        this.self = self;
        this.handler = handler;
    }

    @Override public void waitFor(IActorRef<? extends N> actor, T token) {
        logger.debug("{} waitFor {} / {}", self.sender(TYPE), actor, token);
        wfg.waitFor(self.sender(TYPE), token, actor);
    }

    @Override public void granted(IActorRef<? extends N> actor, T token) {
        logger.debug("{} granted {} / {}", self.sender(TYPE), actor, token);
        wfg.granted(self.sender(TYPE), token, actor);
    }

    @Override public void suspended(Clock<IActorRef<? extends N>> clock,
            MultiSetMap.Immutable<IActorRef<? extends N>, T> waitFors,
            MultiSetMap.Immutable<IActorRef<? extends N>, T> grants) {
        processBatchedWaitFors(waitFors, grants);
        final Deadlock<IActorRef<? extends N>, T> deadlock = wfg.suspend(self.sender(TYPE), clock);
        if(!deadlock.isEmpty()) {
            logger.debug("{} deadlocked: {}", self.sender(TYPE), deadlock);
            handler.apply(self, deadlock);
        }
    }

    private void processBatchedWaitFors(MultiSetMap.Immutable<IActorRef<? extends N>, T> waitFors,
            MultiSetMap.Immutable<IActorRef<? extends N>, T> grants) {
        // Process batch waitFors and grants. Process waitFors first, in case the client
        // does not discharge waitFors locally, but really only batches.
        for(Entry<IActorRef<? extends N>, MultiSet.Immutable<T>> waitForEntry : waitFors.toMap().entrySet()) {
            for(T waitFor : waitForEntry.getValue()) {
                waitFor(waitForEntry.getKey(), waitFor);
            }
        }
        for(Entry<IActorRef<? extends N>, MultiSet.Immutable<T>> grantedEntry : grants.toMap().entrySet()) {
            for(T granted : grantedEntry.getValue()) {
                granted(grantedEntry.getKey(), granted);
            }
        }
    }

}