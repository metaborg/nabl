package mb.statix.concurrent.actors.impl;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Set;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Sets;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.IActorSystem;
import mb.statix.concurrent.actors.TypeTag;

public class ActorSystem implements IActorSystem, IActorInternal<Void> {

    private static final String ID_SEP = "/";

    private static final ILogger logger = LoggerUtils.logger(ActorSystem.class);

    private final Object lock = new Object();
    private final Set<IActorInternal<?>> actors;
    private final IActorScheduler scheduler;
    private volatile boolean running;
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
        this.running = true;
        this.context = new ActorContext();
    }

    @Override public <T> IActor<T> add(String id, TypeTag<T> type, Function1<IActor<T>, T> supplier) {
        final String qid = ID_SEP + escapeId(id);
        return add(null, qid, type, supplier);
    }

    private <T> IActorImpl<T> add(IActorInternal<?> parent, String qid, TypeTag<T> type,
            Function1<IActor<T>, T> supplier) {
        logger.debug("add actor {}", qid);
        final Actor<T> actor = new Actor<>(context, parent, qid, type);
        synchronized(lock) {
            if(!running) {
                throw new IllegalStateException("Actor system already stopped.");
            }
            actors.add(actor);
        }
        logger.debug("added actor {}", qid);
        actor.start(supplier);
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
        return ((IActorInternal<T>) receiver).async();
    }

    @Override public void stop() {
        doStop(null);
    }

    @Override public void cancel() {
        doStop(new InterruptedException());
    }

    @Override public boolean running() {
        synchronized(lock) {
            return running;
        }
    }

    private void doStop(Throwable ex) {
        synchronized(lock) {
            if(!running) {
                throw new IllegalStateException("Actor system not running.");
            }
            running = false;
            for(IActorInternal<?> actor : actors) {
                actor.stop(ex);
            }
        }
        if(ex != null) {
            scheduler.shutdownNow();
        } else {
            scheduler.shutdown();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // IActorInternal
    ///////////////////////////////////////////////////////////////////////////

    @Override public String id() {
        return toString();
    }

    @Override public void start(Function1<IActor<Void>, ? extends Void> supplier) {
        throw new IllegalStateException("Actor system is not started by message.");
    }

    @Override public Void async() {
        throw new IllegalStateException("Actor system has no async interface.");
    }

    @Override public void stop(Throwable ex) {
        doStop(ex);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IActorContext
    ///////////////////////////////////////////////////////////////////////////

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private class ActorContext implements IActorContext {

        @Override public <U> IActorImpl<U> add(String id, TypeTag<U> type, Function1<IActor<U>, U> supplier) {
            final IActorInternal<?> parent = ActorThreadLocals.current.get();
            final String qid = parent.id() + ID_SEP + escapeId(id);
            return ActorSystem.this.add(parent, qid, type, supplier);
        }

        @Override public <T> T async(IActorRef<T> receiver) {
            synchronized(lock) {
                if(!actors.contains(receiver)) {
                    throw new IllegalArgumentException("Actor " + receiver + " not part of this system.");
                }
            }
            return ((IActorInternal<T>) receiver).async();
        }

        @Override public IActorScheduler scheduler() {
            return scheduler;
        }

    }

    @Override public String toString() {
        return "system:/";
    }

}