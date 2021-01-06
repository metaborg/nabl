package mb.statix.concurrent.actors.impl;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Set;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.Sets;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.IActorSystem;
import mb.statix.concurrent.actors.TypeTag;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.ICompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;

public class ActorSystem implements IActorSystem, IActorInternal<Void> {

    private static final String ID_SEP = "/";

    private static final ILogger logger = LoggerUtils.logger(ActorSystem.class);

    private final Object lock = new Object();
    private final Set<IActorInternal<?>> children;
    private final IActorScheduler scheduler;
    private volatile ActorSystemState state;
    private final IActorContext context;

    private final ICompletableFuture<Unit> done;

    public ActorSystem() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public ActorSystem(int parallelism) {
        this(new PriorityBlockingQueueThreadPoolScheduler(parallelism));
    }

    public ActorSystem(IActorScheduler scheduler) {
        this.children = Sets.newHashSet();
        this.scheduler = scheduler;
        this.state = ActorSystemState.RUNNING;
        this.context = new ActorContext();
        this.done = new CompletableFuture<>();

        done.whenComplete((r, ex) -> {
            scheduler.shutdownNow();
        });
    }

    @Override public <T> IActor<T> add(String id, TypeTag<T> type, Function1<IActor<T>, T> supplier) {
        final String qid = ID_SEP + escapeId(id);
        final IActorImpl<T> actor;
        try {
            ActorThreadLocals.current.set(this);
            actor = doAdd(this, qid, type);
            synchronized(lock) {
                if(!state.equals(ActorSystemState.RUNNING)) {
                    throw new IllegalStateException("Actor system already stopped.");
                }
                children.add(actor);
                actor._start(supplier);
            }
        } finally {
            ActorThreadLocals.current.remove();
        }
        return actor;
    }

    private <T> IActorImpl<T> doAdd(IActorInternal<?> parent, String qid, TypeTag<T> type) {
        logger.debug("creating actor {}", qid);
        final IActorImpl<T> actor = new Actor<>(context, parent, qid, type);
        logger.debug("created actor {}", qid);
        return actor;
    }

    private String escapeId(String id) {
        final StringBuilder sb = new StringBuilder();
        final StringCharacterIterator it = new StringCharacterIterator(id);
        while(it.current() != CharacterIterator.DONE) {
            char c = it.current();
            switch(c) {
                case '/':
                case '\\':
                    sb.append('\\').append(c);
                    break;
                default:
                    sb.append(c);
                    break;
            }
            it.next();
        }
        return sb.toString();
    }

    @Override public <T> T async(IActorRef<T> receiver) {
        return ((IActorInternal<T>) receiver)._staticAsync(this);
    }

    @Override public IFuture<Unit> stop() {
        doStop(null);
        return done;
    }

    @Override public IFuture<Unit> cancel() {
        doStop(new InterruptedException());
        return done;
    }

    @Override public boolean running() {
        synchronized(lock) {
            return state.equals(ActorSystemState.RUNNING);
        }
    }

    private void doStop(@SuppressWarnings("unused") Throwable ex) {
        synchronized(lock) {
            switch(state) {
                case RUNNING:
                    state = ActorSystemState.STOPPING;
                    IActorInternal<?> previousCurrent; // HACK Very awkward code to ensure
                                                       // the current actor is properly restored
                                                       // when this is called from an actor.
                                                       // Problem because the system is not an actor,
                                                       // but called directly and with locking.
                    try {
                        previousCurrent = ActorThreadLocals.current.get();
                    } catch(Throwable ex2) {
                        previousCurrent = null;
                    }
                    try {
                        ActorThreadLocals.current.set(this);
                        for(IActorInternal<?> actor : children) {
                            actor._stop(null);
                        }
                    } finally {
                        if(previousCurrent != null) {
                            ActorThreadLocals.current.set(previousCurrent);
                        } else {
                            ActorThreadLocals.current.remove();
                        }
                    }
                    break;
                case STOPPING:
                case STOPPED:
                    break;
                default:
                    throw new IllegalStateException("Unexpected state " + state);
            }
        }
        completeIfDone();
    }

    private void completeIfDone() {
        synchronized(lock) {
            if(!state.equals(ActorSystemState.STOPPING)) {
                return;
            }
            if(!children.isEmpty()) {
                return;
            }
            done.complete(Unit.unit);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // IActorInternal
    ///////////////////////////////////////////////////////////////////////////

    @Override public String id() {
        return toString();
    }

    @Override public void _start(@SuppressWarnings("unused") Function1<IActor<Void>, ? extends Void> supplier) {
        throw new IllegalStateException("Actor system is not started by message.");
    }

    @Override public Void _dynamicAsync() {
        throw new IllegalStateException("Actor system has no async interface.");
    }

    @Override public Void _staticAsync(@SuppressWarnings("unused") IActorInternal<?> system) {
        throw new IllegalStateException("Actor system has no async interface.");
    }

    @Override public void _stop(Throwable ex) {
        doStop(ex);
    }

    @Override public void _childStopped(@SuppressWarnings("unused") Throwable ex) {
        final IActorInternal<?> sender = ActorThreadLocals.current.get();
        synchronized(lock) {
            if(!children.remove(sender)) {
                throw new IllegalStateException("Stopped actor " + sender + " is not a top-level actor.");
            }
        }
        completeIfDone();
    }

    ///////////////////////////////////////////////////////////////////////////
    // IActorContext
    ///////////////////////////////////////////////////////////////////////////

    private class ActorContext implements IActorContext {

        @Override public <U> IActorImpl<U> add(String id, TypeTag<U> type) {
            final IActorInternal<?> self = ActorThreadLocals.current.get();
            final String qid = self.id() + ID_SEP + escapeId(id);
            final IActorImpl<U> actor = doAdd(self, qid, type);
            return actor;
        }

        @Override public <T> T async(IActorRef<T> receiver) {
            return ((IActorInternal<T>) receiver)._dynamicAsync();
        }

        @Override public IActorScheduler scheduler() {
            return scheduler;
        }

    }

    @Override public String toString() {
        return "system:/";
    }

}