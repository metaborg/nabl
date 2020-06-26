package mb.statix.actors;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Maps;

public class ActorSystem {

    private static final ILogger logger = LoggerUtils.logger(ActorSystem.class);

    private final Map<String, Actor<?>> actors;
    private final ExecutorService executorService;

    private final Object lock = new Object();
    private volatile boolean running = false;

    public ActorSystem() {
        this.actors = Maps.newHashMap();
        this.executorService = Executors.newCachedThreadPool();
    }

    public <T> IActor<T> add(String id, TypeTag<T> type, Function1<IActor<T>, T> supplier) {
        logger.info("add actor {}", id);
        final Actor<T> actor = new Actor<>(id, type, supplier);
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

    public void start() {
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

    public void stop() {
        synchronized(lock) {
            if(!running) {
                throw new IllegalStateException("Actor system not running.");
            }
            for(Actor<?> actor : actors.values()) {
                actor.stop();
            }
            executorService.shutdown();
        }
    }

}