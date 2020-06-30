package mb.statix.actors.deadlock;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.SetMultimap;
import mb.statix.actors.IActor;
import mb.statix.actors.IActorRef;

public class DeadlockMonitor<T> implements IDeadlockMonitor<T> {

    private static final ILogger logger = LoggerUtils.logger(DeadlockMonitor.class);
    
    private final IActor<? extends IDeadlockMonitor<T>> self;

    private final WaitForGraph<IActorRef<?>, T> wfg = new WaitForGraph<>();

    public DeadlockMonitor(IActor<? extends IDeadlockMonitor<T>> self) {
        this.self = self;
    }

    @Override public void waitFor(IActorRef<?> source, T token, IActorRef<?> target) {
        wfg.waitFor(source, token, target);
    }

    @Override public void granted(IActorRef<?> source, T token, IActorRef<?> target) {
        wfg.granted(source, token, target);
    }

    @Override public void started(IActorRef<?> actor) {
        Object other = self.async(actor);
        if(!(other instanceof CanDeadlock)) {
            return;
        }
        wfg.add(actor);
    }

    @SuppressWarnings("unchecked") @Override public void suspended(IActorRef<?> actor) {
        Object other = self.async(actor);
        if(!(other instanceof CanDeadlock)) {
            return;
        }
        wfg.suspend(actor).ifPresent(waitFors -> {
            logger.info("{} deadlocked: {}", actor, waitFors);
            logger.info("wfg: {}", wfg);
            ((CanDeadlock<T>) other).deadlocked(waitFors);
        });
    }

    @Override public void resumed(IActorRef<?> actor) {
        Object other = self.async(actor);
        if(!(other instanceof CanDeadlock)) {
            return;
        }
        wfg.activate(actor);
    }

    @Override public void stopped(IActorRef<?> actor) {
        Object other = self.async(actor);
        if(!(other instanceof CanDeadlock)) {
            return;
        }
        final SetMultimap<IActorRef<?>, T> waitFors = wfg.remove(actor);
        if(waitFors.isEmpty()) {
            return;
        }
        // TODO Handle remaining waitFors?
    }

}