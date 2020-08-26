package mb.statix.concurrent.actors.deadlock;

import java.util.Map.Entry;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSetMap;
import mb.nabl2.util.collections.MultiSetMap.Immutable;
import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;

/**
 * Actors can use this class to locally batch wait-for/grant operations and only send them to the deadlock monitor on
 * suspend, thereby reducing messages.
 */
public class DeadlockBatcher<N, S, T> implements IDeadlockMonitor<N, S, T> {

    private static final ILogger logger = LoggerUtils.logger(DeadlockBatcher.class);

    private final IActor<? extends N> self;
    private final IActorRef<? extends IDeadlockMonitor<N, S, T>> dlm;

    private final MultiSetMap.Transient<IActorRef<? extends N>, T> committedWaitFors;
    private final MultiSetMap.Transient<IActorRef<? extends N>, T> pendingWaitFors;
    private final MultiSetMap.Transient<IActorRef<? extends N>, T> pendingGrants;

    public DeadlockBatcher(IActor<? extends N> self, IActorRef<? extends IDeadlockMonitor<N, S, T>> dlm) {
        this.self = self;
        this.dlm = dlm;

        this.committedWaitFors = MultiSetMap.Transient.of();
        this.pendingWaitFors = MultiSetMap.Transient.of();
        this.pendingGrants = MultiSetMap.Transient.of();
    }

    public boolean isWaitingFor(IActorRef<? extends N> actor, T token) {
        return pendingWaitFors.contains(actor, token) || committedWaitFors.contains(actor, token);
    }

    public boolean isWaitingFor(T token) {
        return pendingWaitFors.containsValue(token) || committedWaitFors.containsValue(token);
    }

    @Override public void waitFor(IActorRef<? extends N> actor, T token) {
        logger.debug("wait for {}/{}", actor, token);
        pendingWaitFors.put(actor, token);
    }

    @Override public void granted(IActorRef<? extends N> actor, T token) {
        if(pendingWaitFors.contains(actor, token)) {
            logger.debug("locally granted {}/{}", actor, token);
            pendingWaitFors.remove(actor, token);
        } else {
            if(!committedWaitFors.contains(actor, token)) {
                logger.error("not waiting for granted {}/{}", actor, token);
                throw new IllegalStateException(self + " not waiting for granted " + actor + "/" + token);
            }
            logger.debug("granted {}/{}", actor, token);
            committedWaitFors.remove(actor, token);
            pendingGrants.put(actor, token);
        }
    }

    @Override public void suspended(S state, Clock<IActorRef<? extends N>> clock,
            Immutable<IActorRef<? extends N>, T> waitFors, Immutable<IActorRef<? extends N>, T> grants) {
        processBatchedWaitFors(waitFors, grants);
        self.async(dlm).suspended(state, clock, commitWaitFors(), pendingGrants.clear());
    }

    @Override public void stopped(Clock<IActorRef<? extends N>> clock, Immutable<IActorRef<? extends N>, T> waitFors,
            Immutable<IActorRef<? extends N>, T> grants) {
        processBatchedWaitFors(waitFors, grants);
        self.async(dlm).stopped(clock, commitWaitFors(), pendingGrants.clear());
    }

    private void processBatchedWaitFors(MultiSetMap.Immutable<IActorRef<? extends N>, T> waitFors,
            MultiSetMap.Immutable<IActorRef<? extends N>, T> grants) {
        // Process batch waitFors and grantes. Process waitFors first, in case the client
        // does not discharge waitFors locally, but really only batches.
        for(Entry<IActorRef<? extends N>, MultiSet.Immutable<T>> waitForEntry : waitFors.toMap().entrySet()) {
            for(T waitFor : waitForEntry.getValue()) {
                waitFor(waitForEntry.getKey(), waitFor);
            }
        }
        for(Entry<IActorRef<? extends N>, MultiSet.Immutable<T>> grantedEntry : grants.toMap().entrySet()) {
            for(T granted : grantedEntry.getValue()) {
                waitFor(grantedEntry.getKey(), granted);
            }
        }
    }

    private MultiSetMap.Immutable<IActorRef<? extends N>, T> commitWaitFors() {
        final MultiSetMap.Immutable<IActorRef<? extends N>, T> waitFors = pendingWaitFors.clear();
        for(Entry<IActorRef<? extends N>, MultiSet.Immutable<T>> entry : waitFors.toMap().entrySet()) {
            committedWaitFors.putAll(entry.getKey(), entry.getValue());
        }
        return waitFors;
    }

}