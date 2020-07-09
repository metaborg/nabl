package mb.statix.concurrent.actors.impl;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Maps;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorMonitor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.IActorSystem;
import mb.statix.concurrent.actors.TypeTag;

public class ActorSystem implements IActorSystem {

    private static final ILogger logger = LoggerUtils.logger(ActorSystem.class);

    private final Object lock = new Object();
    private final Map<String, Actor<?>> actors;
    private final ExecutorService executorService;
    private volatile boolean running = false;

    public ActorSystem() {
        this.actors = Maps.newHashMap();
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override public <T> IActor<T> add(String id, TypeTag<T> type, Function1<IActor<T>, T> supplier) {
        return add(null, id, type, supplier);
    }

    private <T> IActor<T> add(@Nullable IActorRef<?> parent, String id, TypeTag<T> type,
            Function1<IActor<T>, T> supplier) {
        logger.info("add actor {}", id);
        final Actor<T> actor = new Actor<>(ActorContext::new, id, type, supplier);
        synchronized(lock) {
            if(actors.containsKey(id)) {
                throw new IllegalArgumentException("Actor with id " + id + " already exists.");
            }
            actors.put(id, actor);
            if(running) {
                actor.run(executorService);
            }
        }
        logger.info("added actor {}", id);
        return actor;
    }

    @Override public void addMonitor(IActorRef<?> actor, IActorRef<? extends IActorMonitor> monitor) {
        ((Actor<?>) actor).addMonitor(async(monitor));
    }

    @Override public <T> T async(IActorRef<T> reciever) {
        return ((Actor<T>) reciever).async(executorService);
    }

    @Override public void start() {
        logger.info("start system");
        synchronized(lock) {
            if(running) {
                throw new IllegalStateException("Actor system already running.");
            }
            for(Actor<?> actor : actors.values()) {
                actor.run(executorService);
            }
            running = true;
        }
        logger.info("started system");
    }

    @Override public void stop() {
        stop(false);
    }

    @Override public void cancel() {
        stop(true);
    }

    private void stop(boolean force) {
        synchronized(lock) {
            if(!running) {
                throw new IllegalStateException("Actor system not running.");
            }
            for(Actor<?> actor : actors.values()) {
                if(force) {
                    actor.cancel();
                } else {
                    actor.stop();
                }
            }
            executorService.shutdown();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private class ActorContext implements IActorContext {

        private final Actor<?> self;
        private final Map<IActorRef, Object> asyncCache = Maps.newHashMap();

        public ActorContext(Actor<?> self) {
            this.self = self;
        }

        @Override public <U> IActor<U> add(String id, TypeTag<U> type, Function1<IActor<U>, U> supplier) {
            return ActorSystem.this.add(self, id, type, supplier);
        }

        @Override public <T> T async(IActorRef<T> reciever) {
            if(!actors.containsValue(reciever)) {
                throw new IllegalArgumentException("Actor " + reciever + " not part of this system.");
            }
            return (T) asyncCache.computeIfAbsent(reciever, r -> ((Actor) r).async(self));
        }

    }

}