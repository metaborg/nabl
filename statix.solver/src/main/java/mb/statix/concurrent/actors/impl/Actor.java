package mb.statix.concurrent.actors.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Queues;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorMonitor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.MessageTags;
import mb.statix.concurrent.actors.TypeTag;
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
    private final Deque<IMessage<T>> messages;
    private final Set<IReturn<?>> returns;

    private final Object monitorsLock;
    private final Set<IActorMonitor> monitors;

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
        this.returns = new HashSet<>();

        this.monitorsLock = new Object();
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

    private void put(IMessage<T> message, boolean priority) throws ActorStoppedException {
        synchronized(lock) {
            if(state.equals(ActorState.STOPPED)) {
                logger.debug("{} received message when already stopped", id);
                throw new ActorStoppedException("Actor " + this + " not running.");
            }
            if(priority) {
                messages.addFirst(message);
            } else {
                messages.addLast(message);
            }
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
                        final Return<?> ret = new Return<>(sender, method, result);
                        message = new Invoke(sender, method, args, ret);
                        returnValue = result;
                        synchronized(lock) {
                            returns.add(ret);
                        }
                    } else {
                        throw new IllegalStateException("Unsupported method called: " + method);
                    }

                    logger.debug("send {} message {}", Actor.this, message);
                    put(message, false);

                    final Set<String> tags = tags(method);
                    sender.forEachMonitor(monitor -> {
                        monitor.sent(sender, this, tags);
                    });

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
                        message = new Invoke(null, method, args,
                                IReturn.of(new AsyncCompletable<>(this.context.executor(), result)));
                        returnValue = result;
                    } else {
                        throw new IllegalStateException("Unsupported method called: " + method);
                    }

                    logger.debug("send {} message {}", Actor.this, message);
                    put(message, false);

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

    @Override public <U> void complete(ICompletable<U> completable, U value, Throwable ex) {
        try {
            put(new Complete<>(completable, value, ex), false);
        } catch(ActorStoppedException e) {
            completable.completeExceptionally(e);
        }
    }

    @Override public <U> IFuture<U> schedule(IFuture<U> future) {
        final CompletableFuture<U> completable = new CompletableFuture<>();
        future.whenComplete((value, ex) -> complete(completable, value, ex));
        return completable;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Run & stop
    ///////////////////////////////////////////////////////////////////////////

    void run(ExecutorService executorService) {
        synchronized(lock) {
            if(!state.equals(ActorState.INIT)) {
                throw new IllegalStateException("Actor already started and/or stopped.");
            }

            state = ActorState.RUNNING;
            executorService.submit(() -> this.run());
        }
    }

    /**
     * Actor main loop.
     */
    private void run() {
        LoggerUtils.setContextId("actor:" + id);
        current.set(this);

        logger.debug("start");

        final T impl = supplier.apply(this);
        if(impl == null || !type.type().isInstance(impl)) {
            throw new IllegalArgumentException(
                    "Supplied implementation " + impl + " does not implement the given interface " + type.type() + ".");
        }

        try {
            forEachMonitor(monitor -> {
                monitor.started(this);
            });
            while(true) {
                final IMessage<T> message;
                synchronized(lock) {
                    while(messages.isEmpty()) {
                        logger.debug("suspend");
                        state = ActorState.WAITING;
                        forEachMonitor(monitor -> {
                            monitor.suspended(this);
                        });
                        lock.wait();
                    }
                    if(state.equals(ActorState.WAITING)) {
                        state = ActorState.RUNNING;
                        forEachMonitor(monitor -> {
                            monitor.resumed(this);
                        });
                        logger.debug("resume");
                    }
                    message = messages.remove();
                }
                logger.debug("deliver message {}", message);
                message.dispatch(impl); // responsible for setting sender, and calling monitor!
            }
        } catch(StopException ex) {
            logger.debug("stopped");
            doStop(null);
        } catch(Throwable ex) {
            logger.error("failed", ex);
            doStop(ex);
        }
    }

    private void doStop(@Nullable Throwable cause) {
        final boolean failed = cause != null;
        final Throwable ex = new ActorStoppedException(cause);
        synchronized(lock) {
            state = ActorState.STOPPED;
            messages.clear();
            for(IReturn<?> ret : returns) {
                try {
                    ret.complete(null, ex);
                } catch(ActorStoppedException e) {
                    // receiver stopped, ignore
                }
            }
            forEachMonitor(monitor -> {
                if(failed) {
                    monitor.failed(this, ex);
                } else {
                    monitor.stopped(this);
                }
            });
        }
    }

    @Override public IActorRef<?> sender() {
        return sender.get();
    }

    @SuppressWarnings("unchecked") @Override public <U> IActorRef<U> sender(TypeTag<U> type) {
        return (IActorRef<U>) sender.get();
    }

    @Override public void stop() {
        try {
            put(new Stop(), true);
        } catch(ActorStoppedException ex) {
            // actor already stopped, do nothing
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Monitors
    ///////////////////////////////////////////////////////////////////////////

    @Override public void addMonitor(IActorMonitor monitor) {
        synchronized(monitorsLock) {
            monitors.add(monitor);
        }
    }

    void forEachMonitor(Action1<? super IActorMonitor> action) {
        synchronized(monitorsLock) {
            monitors.forEach(action::apply);
        }
    }

    private Set<String> tags(Method method) {
        return Optional.ofNullable(method.getAnnotation(MessageTags.class))
                .map(tags -> ImmutableSet.copyOf(tags.value())).orElse(ImmutableSet.of());
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

    @FunctionalInterface
    private interface IReturn<U> {

        void complete(U value, Throwable ex) throws ActorStoppedException;

        public static <U> IReturn<U> of(ICompletable<U> completable) {
            return completable::complete;
        }

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private class Invoke implements IMessage<T> {

        private final IActorRef<?> sender;
        private final Method method;
        private final Object[] args;
        private final IReturn result;

        public Invoke(IActorRef<?> sender, Method method, Object[] args, IReturn<?> result) {
            this.sender = sender;
            this.method = method;
            this.args = args;
            this.result = result;
        }

        @Override public void dispatch(T impl) throws ActorException {
            final Object returnValue;
            try {
                Actor.sender.set(sender);

                method.setAccessible(true);
                returnValue = method.invoke(impl, args);
            } catch(Throwable ex) {
                final ActorException ae = new ActorException("Dispatch failed.", ex);
                if(result != null) {
                    result.complete(null, ae);
                }
                throw ae;
            } finally {
                Actor.sender.remove();
            }

            final Set<String> tags = tags(method);
            forEachMonitor(monitor -> {
                monitor.delivered(Actor.this, sender, tags);
            });

            if(result != null) {
                // NOTE The completion runs on the thread of the receiving actor.
                //      The different async(...) cases dispatch the result as a
                //      message on the thread of the sender, or as a job on the executor.
                ((IFuture<?>) returnValue).whenComplete(result::complete);
            }
        }

        @Override public String toString() {
            return sender + " invokes " + method.getName() + "(" + Arrays.toString(args) + ")";
        }

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private class Return<U> implements IMessage, IReturn<U> {

        private final Actor<?> invoker;
        private final Method method;
        private final ICompletable<U> completable;

        private U value;
        private Throwable ex;

        private Return(Actor<?> sender, Method method, ICompletable<U> completable) {
            this.invoker = sender;
            this.method = method;
            this.completable = completable;
        }

        ///////////////////////////////////////////////////////////////////////
        // complete -- called on the receiver's thread
        ///////////////////////////////////////////////////////////////////////

        @Override public void complete(U value, Throwable ex) throws ActorStoppedException {
            if(!returns.remove(this)) {
                throw new IllegalStateException("Dangling return?");
            }

            this.value = value;
            this.ex = ex;

            logger.debug("send {} message {}", invoker, this);
            invoker.put(this, false);

            final Set<String> tags = tags(method);
            forEachMonitor(monitor -> {
                monitor.sent(Actor.this, invoker, tags);
            });
        }

        ///////////////////////////////////////////////////////////////////////
        // IMessage -- called on the invoker's thread
        ///////////////////////////////////////////////////////////////////////

        @Override public void dispatch(Object impl) {
            // impl is ignored, since the future does not dispatch on the
            // object directly, instead just calling the handler(s).
            try {
                Actor.sender.set(Actor.this);

                completable.complete(value, ex);

                final Set<String> tags = tags(method);
                invoker.forEachMonitor(monitor -> {
                    monitor.delivered(invoker, Actor.this, tags);
                });


            } finally {
                Actor.sender.remove();
            }
        }

        @Override public String toString() {
            return Actor.this + " returns " + completable;
        }

    }

    private class Complete<U> implements IMessage<T> {

        private final ICompletable<U> completable;
        private final U value;
        private final Throwable ex;

        private Complete(ICompletable<U> completable, U value, Throwable ex) {
            this.completable = completable;
            this.value = value;
            this.ex = ex;
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
            } finally {
                Actor.sender.remove();
            }
        }

        @Override public String toString() {
            return "complete " + completable;
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