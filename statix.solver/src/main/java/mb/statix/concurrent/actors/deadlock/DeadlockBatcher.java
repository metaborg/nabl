package mb.statix.concurrent.actors.deadlock;

import java.util.Map.Entry;

import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.collection.MultiSetMap;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;

/**
 * Actors can use this class to locally batch wait-for/grant operations and only send them to the deadlock monitor on
 * suspend, thereby reducing messages.
 */
public class DeadlockBatcher<N, T> {

    private static final ILogger logger = LoggerUtils.logger(DeadlockBatcher.class);

    private final IActor<? extends N> self;
    private final IActorRef<? extends IDeadlockMonitor<N>> dlm;

    private MultiSet.Immutable<T> waitFors;
    private MultiSetMap.Immutable<IActorRef<? extends N>, T> waitForsByActor;
    private MultiSet.Immutable<IActorRef<? extends N>> committedWaitFors;
    private MultiSet.Immutable<IActorRef<? extends N>> pendingWaitFors;
    private MultiSet.Immutable<IActorRef<? extends N>> pendingGrants;

    public DeadlockBatcher(IActor<? extends N> self, IActorRef<? extends IDeadlockMonitor<N>> dlm) {
        this.self = self;
        this.dlm = dlm;

        this.waitFors = MultiSet.Immutable.of();
        this.waitForsByActor = MultiSetMap.Immutable.of();
        this.committedWaitFors = MultiSet.Immutable.of();
        this.pendingWaitFors = MultiSet.Immutable.of();
        this.pendingGrants = MultiSet.Immutable.of();
    }

    public boolean isWaiting() {
        return !waitFors.isEmpty();
    }

    public boolean isWaitingFor(T token) {
        return waitFors.contains(token);
    }

    public MultiSet.Immutable<T> getTokens(IActorRef<? extends N> actor) {
        return waitForsByActor.get(actor);
    }

    public MultiSet.Immutable<T> getAllTokens() {
        return waitFors;
    }

    public void waitFor(IActorRef<? extends N> actor, T token) {
        logger.debug("{} wait for {}/{}", self, actor, token);
        waitFors = waitFors.add(token);
        waitForsByActor = waitForsByActor.put(actor, token);
        pendingWaitFors = pendingWaitFors.add(actor);
    }

    public void granted(IActorRef<? extends N> actor, T token) {
        if(!waitForsByActor.contains(actor, token)) {
            logger.error("{} not waiting for granted {}/{}", self, actor, token);
            throw new IllegalStateException(self + " not waiting for granted " + actor + "/" + token);
        }
        waitFors = waitFors.remove(token);
        waitForsByActor = waitForsByActor.remove(actor, token);
        if(pendingWaitFors.contains(actor)) {
            logger.debug("{} locally granted {}/{}", self, actor, token);
            pendingWaitFors = pendingWaitFors.remove(actor);
        } else if(committedWaitFors.contains(actor)) {
            logger.debug("{} granted {}/{}", self, actor, token);
            final MultiSet.Transient<IActorRef<? extends N>> _committedWaitFors = committedWaitFors.melt();
            final int n = _committedWaitFors.remove(actor);
            if(n == 1) {
                pendingGrants = pendingGrants.add(actor);
            }
            committedWaitFors = _committedWaitFors.freeze();
        } else {
            logger.error("waitFors out of sync with {pending,committed}WaitFors.");
            throw new IllegalStateException("waitFors out of sync with {pending,committed}WaitFors.");
        }
    }

    public void suspended(Clock<IActorRef<? extends N>> clock) {
        self.async(dlm).suspended(clock, commitWaitFors(), commitGrants());
    }

    private MultiSet.Immutable<IActorRef<? extends N>> commitWaitFors() {
        final MultiSet.Transient<IActorRef<? extends N>> newWaitFors = MultiSet.Transient.of();
        final MultiSet.Transient<IActorRef<? extends N>> _committedWaitFors = committedWaitFors.melt();
        for(Entry<IActorRef<? extends N>, Integer> entry : pendingWaitFors.entrySet()) {
            final int n = _committedWaitFors.add(entry.getKey(), entry.getValue());
            if(n == 0) {
                newWaitFors.add(entry.getKey());
            }
        }
        committedWaitFors = _committedWaitFors.freeze();
        pendingWaitFors = MultiSet.Immutable.of();
        return newWaitFors.freeze();
    }

    private MultiSet.Immutable<IActorRef<? extends N>> commitGrants() {
        final MultiSet.Immutable<IActorRef<? extends N>> newGrants = pendingGrants;
        pendingGrants = MultiSet.Immutable.of();
        return newGrants;
    }

}