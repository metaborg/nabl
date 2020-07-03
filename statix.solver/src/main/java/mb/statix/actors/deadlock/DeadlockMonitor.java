package mb.statix.actors.deadlock;

import org.metaborg.util.functions.Action2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.SetMultimap;
import mb.nabl2.util.collections.IRelation3;
import mb.nabl2.util.collections.IRelation3.Immutable;
import mb.statix.actors.IActor;
import mb.statix.actors.IActorRef;

public class DeadlockMonitor<T> implements IDeadlockMonitor<T> {

    private static final ILogger logger = LoggerUtils.logger(DeadlockMonitor.class);

    private final IActor<? extends IDeadlockMonitor<T>> self;

    private final WaitForGraph<IActorRef<?>, T> wfg = new WaitForGraph<>();
    private Action2<IActor<?>, Immutable<IActorRef<?>, T, IActorRef<?>>> handler;

    public DeadlockMonitor(IActor<? extends IDeadlockMonitor<T>> self,
            Action2<IActor<?>, IRelation3.Immutable<IActorRef<?>, T, IActorRef<?>>> handler) {
        this.self = self;
        this.handler = handler;
    }

    @Override public void waitFor(IActorRef<?> source, T token, IActorRef<?> target) {
        wfg.waitFor(source, token, target);
    }

    @Override public void granted(IActorRef<?> source, T token, IActorRef<?> target) {
        wfg.granted(source, token, target);
    }

    @Override public void started(IActorRef<?> actor) {
        wfg.add(actor);
    }

    @Override public void suspended(IActorRef<?> actor) {
        wfg.suspend(actor).ifPresent(waitFors -> {
            logger.info("{} deadlocked: {}", actor, waitFors);
            logger.info("wfg: {}", wfg);
            handler.apply(self, waitFors);
        });
    }

    @Override public void resumed(IActorRef<?> actor) {
        wfg.activate(actor);
    }

    @Override public void stopped(IActorRef<?> actor) {
        final SetMultimap<IActorRef<?>, T> waitFors = wfg.remove(actor);
        if(waitFors.isEmpty()) {
            return;
        }
        // TODO Handle remaining waitFors?
        //      Messages resolving these may be in progress...
    }

}