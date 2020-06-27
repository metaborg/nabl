package mb.statix.actors.deadlock;

import io.usethesource.capsule.SetMultimap;
import mb.statix.actors.IActor;
import mb.statix.actors.IActorRef;

public class DeadlockMonitor<T> implements IDeadlockMonitor<T> {

    @SuppressWarnings("unused") private final IActor<IDeadlockMonitor<T>> self;

    private final WaitForGraph<IActorRef<?>, T> wfg = new WaitForGraph<>();

    public DeadlockMonitor(IActor<IDeadlockMonitor<T>> self) {
        this.self = self;
    }

    @Override public void waitFor(IActorRef<?> source, T token, IActorRef<?> target) {
        if(!(source.get() instanceof CanDeadlock)) {
            throw new IllegalArgumentException("Actor " + source + " must implement CanDeadlock");
        }
        if(!(target.get() instanceof CanDeadlock)) {
            throw new IllegalArgumentException("Actor " + source + " must implement CanDeadlock");
        }
        wfg.waitFor(source, token, target);
    }

    @Override public void granted(IActorRef<?> source, T token, IActorRef<?> target) {
        if(!(source.get() instanceof CanDeadlock)) {
            throw new IllegalArgumentException("Actor " + source + " must implement CanDeadlock");
        }
        if(!(target.get() instanceof CanDeadlock)) {
            throw new IllegalArgumentException("Actor " + source + " must implement CanDeadlock");
        }
        wfg.granted(source, token, target);
    }

    @Override public void started(IActorRef<?> actor) {
        wfg.add(actor);
    }

    @SuppressWarnings("unchecked") @Override public void suspended(IActorRef<?> actor) {
        if(!(actor.get() instanceof CanDeadlock)) {
            return;
        }
        final SetMultimap<IActorRef<?>, T> waitFors = wfg.suspend(actor);
        if(waitFors.isEmpty()) {
            return;
        }
        ((CanDeadlock<T>) actor.get()).deadlocked(waitFors);
    }

    @Override public void resumed(IActorRef<?> actor) {
        if(!(actor.get() instanceof CanDeadlock)) {
            return;
        }
        wfg.activate(actor);
    }

    @Override public void stopped(IActorRef<?> actor) {
        final SetMultimap<IActorRef<?>, T> waitFors = wfg.remove(actor);
        if(waitFors.isEmpty()) {
            return;
        }
        // TODO Handle remaining waitFors
    }

}