package mb.statix.concurrent.actors.deadlock;

import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.functions.Action2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;

public class DeadlockMonitor<N> implements IDeadlockMonitor<N> {

    private static final ILogger logger = LoggerUtils.logger(DeadlockMonitor.class);

    private final TypeTag<? extends N> TYPE = TypeTag.of(Object.class);

    private final IActor<? extends IDeadlockMonitor<N>> self;

    private final WaitForGraph<IActorRef<? extends N>> wfg = new WaitForGraph<>();
    private Action2<IActor<?>, Deadlock<IActorRef<? extends N>>> handler;

    public DeadlockMonitor(IActor<? extends IDeadlockMonitor<N>> self,
            Action2<IActor<?>, Deadlock<IActorRef<? extends N>>> handler) {
        this.self = self;
        this.handler = handler;
    }

    @Override public void waitFor(IActorRef<? extends N> actor) {
        logger.debug("{} waitFor {}", self.sender(TYPE), actor);
        wfg.waitFor(self.sender(TYPE), actor);
    }

    @Override public void granted(IActorRef<? extends N> actor) {
        logger.debug("{} granted {}", self.sender(TYPE), actor);
        wfg.granted(self.sender(TYPE), actor);
    }

    @Override public void suspended(Clock<IActorRef<? extends N>> clock,
            MultiSet.Immutable<IActorRef<? extends N>> waitFors, MultiSet.Immutable<IActorRef<? extends N>> grants) {
        processBatchedWaitFors(waitFors, grants);
        final Deadlock<IActorRef<? extends N>> deadlock = wfg.suspend(self.sender(TYPE), clock);
        if(!deadlock.isEmpty()) {
            logger.debug("{} deadlocked: {}", self.sender(TYPE), deadlock);
            handler.apply(self, deadlock);
        }
    }

    private void processBatchedWaitFors(MultiSet.Immutable<IActorRef<? extends N>> waitFors,
            MultiSet.Immutable<IActorRef<? extends N>> grants) {
        // Process batch waitFors and grants. Process waitFors first, in case the client
        // does not discharge waitFors locally, but really only batches.
        for(IActorRef<? extends N> waitFor : waitFors) {
            waitFor(waitFor);
        }
        for(IActorRef<? extends N> granted : grants) {
            granted(granted);
        }
    }

}