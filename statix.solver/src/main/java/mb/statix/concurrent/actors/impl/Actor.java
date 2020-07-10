package mb.statix.concurrent.actors.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Queues;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorMonitor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;
import mb.statix.concurrent.actors.deadlock.Relevant;
import mb.statix.concurrent.actors.futures.AsyncCompletable;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.ICompletable;
import mb.statix.concurrent.actors.futures.ICompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;

class Actor<T> implements IActorRef<T>, IActor<T> {

    private static final ILogger logger = LoggerUtils.logger(Actor.class);

    private final IActorContext context;
    private final String id;
    private final TypeTag<T> type;
    private final Function1<IActor<T>, ? extends T> supplier;

    private final Object lock;
    private volatile ActorState state;
    private final Queue<IMessage<T>> messages;
    private final Set<IActorMonitor> monitors;
    private Future<?> task;

    final T asyncSystem;
    final T asyncActor;

    static final ThreadLocal<Actor<?>> current = ThreadLocal.withInitial(() -> {
        throw new IllegalStateException("Cannot get current actor.");
    });

    private static final ThreadLocal<IActorRef<?>> sender = ThreadLocal.withInitial(() -> {
        throw new IllegalStateException("Cannot get sender when not in message processing context.");
    });

    Actor(IActorContext context, String id, TypeTag<T> type, Function1<IActor<T>, ? extends T> supplier) {
        this.context = context;
        this.id = id;
        this.type = type;
        this.supplier = supplier;

        this.lock = new Object();
        this.state = ActorState.INIT;
        this.messages = Queues.newArrayDeque();
        this.monitors = new HashSet<>();

        this.asyncSystem = newAsyncSystem();
        this.asyncActor = newAsyncActor();

    }

    @Override public String id() {
        return id;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Start actors
    ///////////////////////////////////////////////////////////////////////////

    @Override public <U> IActor<U> add(String id, TypeTag<U> type, Function1<IActor<U>, U> supplier) {
        return context.add(id, type, supplier);
    }

    private void put(IMessage<T> message) {
        synchronized(lock) {
            if(state.equals(ActorState.STOPPED)) {
                logger.warn("{} received message when already stopped", id);
                // FIXME Ignore, or signal error?
                return;
            }
            messages.add(message);
            lock.notify();
        }
    }

    @SuppressWarnings("unchecked") private T newAsyncActor() {
        return (T) Proxy.newProxyInstance(this.type.type().getClassLoader(), new Class[] { this.type.type() },
                (proxy, method, args) -> {
                    if(method.getDeclaringClass().equals(Object.class)) {
                        return method.invoke(this, args);
                    }

                    final Actor<?> sender = Actor.current.get();

                    // This runs on the sender's thread!
                    // ASSERT ActorState != DONE
                    final Class<?> returnType = method.getReturnType();
                    final IMessage<T> message;
                    final Object returnValue;

                    if(Void.TYPE.isAssignableFrom(returnType)) {
                        message = new Invoke(sender, method, args, null);
                        returnValue = null;
                    } else if(IFuture.class.isAssignableFrom(returnType)) {
                        final ICompletableFuture<?> result = new CompletableFuture<>();
                        message = new Invoke(sender, method, args, new Return<>(sender, method, result));
                        returnValue = result;
                    } else {
                        throw new IllegalStateException("Unsupported method called: " + method);
                    }

                    logger.info("send {} message {}", Actor.this, message);
                    put(message);

                    final Relevant marker;
                    if((marker = method.getAnnotation(Relevant.class)) != null && marker.value().length > 0) {
                        for(IActorMonitor monitor : sender.monitors) {
                            monitor.sent(sender, this, ImmutableSet.copyOf(marker.value()));
                        }
                    }

                    return returnValue;
                });
    }

    @SuppressWarnings("unchecked") private T newAsyncSystem() {
        return (T) Proxy.newProxyInstance(this.type.type().getClassLoader(), new Class[] { this.type.type() },
                (proxy, method, args) -> {
                    if(method.getDeclaringClass().equals(Object.class)) {
                        return method.invoke(this, args);
                    }

                    // This runs on the sender's thread!
                    // ASSERT ActorState != DONE
                    final Class<?> returnType = method.getReturnType();
                    final IMessage<T> message;
                    final Object returnValue;
                    if(Void.TYPE.isAssignableFrom(returnType)) {
                        message = new Invoke(null, method, args, null);
                        returnValue = null;
                    } else if(IFuture.class.isAssignableFrom(returnType)) {
                        final ICompletableFuture<?> result = new CompletableFuture<>();
                        message =
                                new Invoke(null, method, args, new AsyncCompletable<>(this.context.executor(), result));
                        returnValue = result;
                    } else {
                        throw new IllegalStateException("Unsupported method called: " + method);
                    }

                    logger.info("send {} message {}", Actor.this, message);
                    put(message);

                    // no sender, so cannot call senders monitors

                    return returnValue;
                });
    }

    @Override public <U> U async(IActorRef<U> receiver) {
        return context.async(receiver);
    }

    @Override public T local() {
        return asyncActor;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Run & stop
    ///////////////////////////////////////////////////////////////////////////

    void run(ExecutorService executorService) {
        //        logger.info("start {}", id);
        synchronized(lock) {
            if(!state.equals(ActorState.INIT)) {
                throw new IllegalStateException("Actor already started and/or stopped.");
            }

            state = ActorState.RUNNING;
            task = executorService.submit(() -> this.run());
        }
    }

    /**
     * Actor main loop.
     */
    private void run() {
        LoggerUtils.setContextId("actor:" + id);
        current.set(this);

        logger.info("start");

        final T impl = supplier.apply(this);
        if(impl == null || !type.type().isInstance(impl)) {
            throw new IllegalArgumentException(
                    "Supplied implementation " + impl + " does not implement the given interface " + type.type() + ".");
        }

        try {
            synchronized(lock) {
                for(IActorMonitor monitor : monitors) {
                    monitor.started(this);
                }
            }
            while(true) {
                final IMessage<T> message;
                synchronized(lock) {
                    while(messages.isEmpty()) {
                        logger.info("suspend");
                        state = ActorState.WAITING;
                        for(IActorMonitor monitor : monitors) {
                            monitor.suspended(this);
                        }
                        lock.wait();
                    }
                    if(state.equals(ActorState.WAITING)) {
                        state = ActorState.RUNNING;
                        for(IActorMonitor monitor : monitors) {
                            monitor.resumed(this);
                        }
                        logger.info("resume");
                    }
                    message = messages.remove();
                }
                logger.info("deliver message {}", message);
                message.dispatch(impl); // responsible for setting sender, and calling monitor!
            }
        } catch(InterruptedException ex) {
            logger.info("interrupted");
        } catch(StopException ex) {
            logger.info("stopped");
        } catch(ActorException ex) {
            logger.info("failed", ex);
        } finally {
            synchronized(lock) {
                state = ActorState.STOPPED;
                messages.clear();
                for(IActorMonitor monitor : monitors) {
                    monitor.stopped(this);
                }
            }
        }
    }

    @Override public IActorRef<?> sender() {
        return sender.get();
    }

    @SuppressWarnings("unchecked") @Override public <U> IActorRef<U> sender(TypeTag<U> type) {
        return (IActorRef<U>) sender.get();
    }

    @Override public void stop() {
        put(new Stop());
    }

    void cancel() {
        synchronized(lock) {
            task.cancel(true);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Monitors
    ///////////////////////////////////////////////////////////////////////////

    @Override public void addMonitor(IActorMonitor monitor) {
        synchronized(lock) {
            monitors.add(monitor);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // toString
    ///////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return "actor:" + id;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Invoke message implementation
    ///////////////////////////////////////////////////////////////////////////

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private class Invoke implements IMessage<T> {

        private final IActorRef<?> sender;
        private final Method method;
        private final Object[] args;
        private final ICompletable result;

        public Invoke(IActorRef<?> sender, Method method, Object[] args, ICompletable<?> result) {
            this.sender = sender;
            this.method = method;
            this.args = args;
            this.result = result;
        }

        @Override public void dispatch(T impl) throws ActorException {
            try {
                Actor.sender.set(sender);

                method.setAccessible(true);
                final Object returnValue = method.invoke(impl, args);

                final Relevant marker;
                if((marker = method.getAnnotation(Relevant.class)) != null && marker.value().length > 0) {
                    for(IActorMonitor monitor : monitors) {
                        monitor.delivered(Actor.this, sender, ImmutableSet.copyOf(marker.value()));
                    }
                }

                if(result != null) {
                    // NOTE The completion runs on the thread of the receiving actor.
                    //      The different async(...) cases dispatch the result as a
                    //      message on the thread of the sender, or as a job on the executor.
                    ((IFuture<?>) returnValue).whenComplete((r, ex) -> {
                        if(ex != null) {
                            result.completeExceptionally(ex);
                        } else {
                            result.complete(r);
                        }
                    });
                }
            } catch(ReflectiveOperationException | IllegalArgumentException ex) {
                throw new ActorException("Dispatch failed.", ex);
            } finally {
                Actor.sender.remove();
            }
        }

        @Override public String toString() {
            return sender + " invokes " + method.getName() + "(" + Arrays.toString(args) + ")";
        }

    }

    @SuppressWarnings("rawtypes")
    private class Return<U> implements ICompletable<U>, IMessage {

        private final Actor<?> sender;
        private final Method method;
        private final ICompletable<U> completable;

        private U value;
        private Throwable ex;

        private Return(Actor<?> sender, Method method, ICompletable<U> completable) {
            this.sender = sender;
            this.method = method;
            this.completable = completable;
        }

        ///////////////////////////////////////////////////////////////////////
        // Completable -- called on the receiver's thread
        ///////////////////////////////////////////////////////////////////////

        @SuppressWarnings({ "unchecked" }) @Override public void complete(U value, Throwable ex) {
            this.value = value;
            this.ex = ex;

            logger.info("send {} message {}", sender, this);
            sender.put(this);

            final Relevant marker;
            if((marker = method.getAnnotation(Relevant.class)) != null && marker.value().length > 0) {
                for(IActorMonitor monitor : monitors) {
                    monitor.sent(Actor.this, sender, ImmutableSet.copyOf(marker.value()));
                }
            }

        }

        @Override public boolean isDone() {
            return completable.isDone();
        }

        ///////////////////////////////////////////////////////////////////////
        // IMessage -- called on the (original) sender's thread
        ///////////////////////////////////////////////////////////////////////

        @Override public void dispatch(Object impl) {
            // impl is ignored, since the future does not dispatch on the
            // object directly, instead just calling the handler(s).
            try {
                Actor.sender.set(Actor.this);

                completable.complete(value, ex);

                final Relevant marker;
                if((marker = method.getAnnotation(Relevant.class)) != null && marker.value().length > 0) {
                    for(IActorMonitor monitor : sender.monitors) {
                        monitor.delivered(sender, Actor.this, ImmutableSet.copyOf(marker.value()));
                    }
                }


            } finally {
                Actor.sender.remove();
            }
        }

        @Override public String toString() {
            return Actor.this + " returns " + completable;
        }

    }

    private class Stop implements IMessage<T> {

        @Override public void dispatch(T impl) throws StopException {
            throw new StopException();
        }

        @Override public String toString() {
            return "stop";
        }

    }

}