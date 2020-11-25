package mb.statix.concurrent.actors.impl;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Set;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Sets;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorMonitor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.IActorSystem;
import mb.statix.concurrent.actors.TypeTag;

public class ActorSystem implements IActorSystem {

    private static final String ID_SEP = "/";

    private static final ILogger logger = LoggerUtils.logger(ActorSystem.class);

    private final Object lock = new Object();
    private final Set<Actor<?>> actors;
    private final IActorScheduler scheduler;
    private volatile ActorSystemState state;
    private final IActorContext context;

    public ActorSystem() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public ActorSystem(int parallelism) {
        this(new PriorityBlockingQueueThreadPoolScheduler(parallelism));
    }

    public ActorSystem(IActorScheduler scheduler) {
        this.actors = Sets.newHashSet();
        this.scheduler = scheduler;
        this.state = ActorSystemState.INIT;
        this.context = new ActorContext();
    }

    @Override public <T> IActor<T> add(String id, TypeTag<T> type, Function1<IActor<T>, T> supplier) {
        final String qid = ID_SEP + escapeId(id);
        return add(null, qid, type, supplier);
    }

    private <T> IActor<T> add(@Nullable IActorRef<?> parent, String qid, TypeTag<T> type,
            Function1<IActor<T>, T> supplier) {
        logger.debug("add actor {}", qid);
        final Actor<T> actor = new Actor<>(context, qid, type, supplier);
        synchronized(lock) {
            if(state.equals(ActorSystemState.STOPPED)) {
                throw new IllegalStateException("Actor system already stopped.");
            }
            actors.add(actor);
            if(state.equals(ActorSystemState.RUNNING)) {
                actor.start();
            }
        }
        logger.debug("added actor {}", qid);
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

    @Override public void addMonitor(IActorRef<?> actor, IActorRef<? extends IActorMonitor> monitor) {
        ((Actor<?>) actor).addMonitor(async(monitor));
    }

    @Override public <T> T async(IActorRef<T> receiver) {
        return ((Actor<T>) receiver).asyncSystem;
    }

    @Override public void start() {
        logger.debug("start system");
        synchronized(lock) {
            if(!state.equals(ActorSystemState.INIT)) {
                throw new IllegalStateException("Actor system already started.");
            }
            state = ActorSystemState.RUNNING;
            for(Actor<?> actor : actors) {
                actor.start();
            }
        }
        logger.debug("started system");
    }

    @Override public void stop() {
        synchronized(lock) {
            if(!state.equals(ActorSystemState.RUNNING)) {
                throw new IllegalStateException("Actor system not started.");
            }
            state = ActorSystemState.STOPPED;
            for(Actor<?> actor : actors) {
                actor.stop();
            }
            scheduler.shutdown();
        }
    }

    @Override public void cancel() {
        synchronized(lock) {
            if(!state.equals(ActorSystemState.RUNNING)) {
                throw new IllegalStateException("Actor system not started.");
            }
            state = ActorSystemState.STOPPED;
            for(Actor<?> actor : actors) {
                actor.stop();
            }
            scheduler.shutdownNow();
        }
    }

    @Override public boolean running() {
        return state.equals(ActorSystemState.RUNNING);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IActorContext
    ///////////////////////////////////////////////////////////////////////////

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private class ActorContext implements IActorContext {

        @Override public <U> IActor<U> add(String id, TypeTag<U> type, Function1<IActor<U>, U> supplier) {
            final Actor<?> parent = Actor.current.get();
            final String qid = parent.id() + ID_SEP + escapeId(id);
            return ActorSystem.this.add(parent, qid, type, supplier);
        }

        @Override public <T> T async(IActorRef<T> receiver) {
            if(!actors.contains(receiver)) {
                throw new IllegalArgumentException("Actor " + receiver + " not part of this system.");
            }
            return (T) ((Actor) receiver).asyncActor;
        }

        @Override public IActorScheduler scheduler() {
            return scheduler;
        }

    }

}