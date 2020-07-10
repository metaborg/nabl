package mb.statix.concurrent.actors.deadlock;

import org.metaborg.util.functions.Action2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;

public class DeadlockMonitor<N, S, T> implements IDeadlockMonitor<N, S, T> {

    private static final ILogger logger = LoggerUtils.logger(DeadlockMonitor.class);

    private final TypeTag<? extends N> TYPE = TypeTag.of(Object.class);

    private final IActor<? extends IDeadlockMonitor<N, S, T>> self;

    private final WaitForGraph<IActorRef<? extends N>, S, T> wfg = new WaitForGraph<>();
    private Action2<IActor<?>, Deadlock<IActorRef<? extends N>, S, T>> handler;

    public DeadlockMonitor(IActor<? extends IDeadlockMonitor<N, S, T>> self,
            Action2<IActor<?>, Deadlock<IActorRef<? extends N>, S, T>> handler) {
        this.self = self;
        this.handler = handler;
    }

    @Override public void waitFor(IActorRef<? extends N> actor, T token) {
        logger.info("{} waitFor {} / {}", self.sender(TYPE), actor, token);
        wfg.waitFor(self.sender(TYPE), token, actor);
    }

    @Override public void granted(IActorRef<? extends N> actor, T token) {
        logger.info("{} granted {} / {}", self.sender(TYPE), actor, token);
        wfg.granted(self.sender(TYPE), token, actor);
    }

    @Override public void suspended(S state, Clock<IActorRef<? extends N>> clock) {
        wfg.suspend(self.sender(TYPE), state, clock).flatMap(o -> o).ifPresent(deadlock -> {
            logger.info("{} deadlocked: {}", self.sender(TYPE), deadlock);
            logger.info("wfg: {}", wfg);
            handler.apply(self, deadlock);
        });
    }

    @Override public void stopped(Clock<IActorRef<? extends N>> clock) {
        wfg.remove(self.sender(TYPE), clock);
        logger.warn("{} stopped", self.sender(TYPE));
    }

}