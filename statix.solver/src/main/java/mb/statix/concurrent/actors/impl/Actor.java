package mb.statix.concurrent.actors.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.metaborg.util.functions.Action2;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import mb.nabl2.util.CapsuleUtil;
import mb.statix.concurrent.actors.ControlMessage;
import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorMonitor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.IActorStats;
import mb.statix.concurrent.actors.MessageTags;
import mb.statix.concurrent.actors.TypeTag;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.ICompletable;
import mb.statix.concurrent.actors.futures.ICompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;

class Actor<T> implements IActorImpl<T>, Runnable {

    private static final ILogger logger = LoggerUtils.logger(Actor.class);

    private final IActorContext context;
    private final String id;
    private final TypeTag<T> type;
    private final IActorInternal<?> parent;
    private final java.util.Set<IActorInternal<?>> children;

    private final AtomicBoolean running;
    private final AtomicReference<Runnable> scheduledTask;

    private volatile ActorState state;
    private final AtomicInteger priority;
    private final Deque<Message> messages;

    private T impl;
    private IActorMonitor monitor;

    private volatile Thread thread = null;

    private static final ThreadLocal<IActorRef<?>> sender = ThreadLocal.withInitial(() -> {
        logger.error("Cannot get sender when not in message processing context.");
        throw new IllegalStateException("Cannot get sender when not in message processing context.");
    });

    private Stats stats = new Stats();

    Actor(IActorContext context, IActorInternal<?> parent, String id, TypeTag<T> type) {
        this.context = context;
        this.parent = parent;
        this.id = id;
        this.type = type;
        this.children = new HashSet<>();

        this.running = new AtomicBoolean();
        this.scheduledTask = new AtomicReference<>();

        this.state = ActorState.INITIAL;
        this.priority = new AtomicInteger(0);
        this.messages = new ConcurrentLinkedDeque<>();

        this.async = newAsync();
    }

    @Override public String id() {
        return id;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Run -- main loop
    ///////////////////////////////////////////////////////////////////////////

    @Override public void run() {
        try {
            initThread();

            scheduledTask.set(null);

            int startPriority = this.priority.get();
            stats.maxPendingMessagesOnActivate = Math.max(stats.maxPendingMessagesOnActivate, startPriority);

            while(true) {
                stats.maxPendingMessages = Math.max(stats.maxPendingMessages, priority.get());

                if(context.scheduler().preempt(priority.get())) {
                    finalizeThread();
                    context.scheduler().schedule(this, priority.get(), scheduledTask);
                    return;
                }

                final Message message = messages.poll();
                if(message != null) {
                    stats.messages += 1;
                    this.priority.decrementAndGet();
                    try {
                        message.dispatch();
                    } catch(Throwable ex) {
                        doStop(ex);
                    }
                } else {
                    if(state.equals(ActorState.RUNNING)) {
                        logger.debug("suspend");
                        stats.suspended += 1;

                        state = ActorState.WAITING;
                        monitor.suspended();
                    }

                    finalizeThread();

                    if(!running.compareAndSet(true, false)) {
                        doStop(new IllegalStateException("Unexpected state, should be running."));
                    }

                    if(!messages.isEmpty() && running.compareAndSet(false, true)) {
                        initThread();
                    } else {
                        return;
                    }
                }
            }
        } catch(Throwable ex) {
            logger.error("THIS SHOUD NOT HAPPEN", ex);
        }
    }

    private void initThread() {
        if(thread != null) {
            logger.error("Actor already running on another thread.");
            throw new IllegalStateException("Actor already running on another thread.");
        }
        thread = Thread.currentThread();
        ActorThreadLocals.current.set(this);
        LoggerUtils.setContextId(this.toString());
    }

    private void finalizeThread() {
        LoggerUtils.clearContextId();
        ActorThreadLocals.current.remove();
        thread = null;
    }

    private void scheduleIfNotRunning() {
        if(running.compareAndSet(false, true)) {
            logger.debug("resume {}", this);
            context.scheduler().schedule(this, priority.get(), scheduledTask);
        } else {
            final Runnable oldTask = scheduledTask.getAndSet(null);
            if(oldTask != null) {
                // only the case when running == true, but the thread is not
                // running the message loop---but task may be unqueued, and/or
                // the thread may be started already

                // if the thread starts in the meantime, it sets the scheduledTask
                // to null, and we should not set it to anything anymore. But, if
                // the thread started, it also means the task is not active anymore,
                // so rescheduling will fail.

                context.scheduler().reschedule(oldTask, priority.get(), scheduledTask);
            }
        }
    }

    private void doStop(Throwable ex) {
        switch(state) {
            case INITIAL:
            case RUNNING:
            case WAITING:
                state = ActorState.STOPPED;
                // FIXME cleanup?
                // FIXME Fail fail children?
                if(ex != null) {
                    monitor.failed(ex);
                } else {
                    monitor.stopped();
                }
                break;
            case STOPPED:
                break;
            default:
                throw new IllegalStateException("Unexpected state " + state);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Messages
    ///////////////////////////////////////////////////////////////////////////

    @FunctionalInterface
    private interface Message {

        void dispatch() throws ActorException;

    }

    private void put(Message message) {
        priority.incrementAndGet();
        messages.add(message);
        scheduleIfNotRunning();
    }

    @Override public void start(Function1<IActor<T>, ? extends T> supplier) {
        final IActorInternal<?> sender = ActorThreadLocals.current.get();
        put(() -> {
            if(!state.equals(ActorState.INITIAL)) {
                throw new ActorException("Cannot start actor that was already started.");
            }
            if(!sender.equals(parent)) {
                throw new ActorException("Actor can only be started by parent.");
            }

            try {
                impl = supplier.apply(this);
            } catch(Throwable ex) {
                throw new ActorException("Creating actor implementation failed.", ex);
            }

            if(IActorMonitor.class.isAssignableFrom(impl.getClass())) {
                monitor = (IActorMonitor) impl;
            } else {
                monitor = new IActorMonitor() {};
            }

            state = ActorState.RUNNING;

            try {
                monitor.started();
            } catch(Throwable ex) {
                throw new ActorException("Monitor failed.", ex);
            }
        });
    }

    private void invoke(final IActorInternal<?> sender, final Method method, final Object[] args,
            Action2<Object, Throwable> result) {
        put(() -> {
            switch(state) {
                case INITIAL:
                    throw new ActorException("Cannot invoke on actor that was not started.");
                case RUNNING:
                    break;
                case WAITING:
                    if(isPrimaryMessage(method)) {
                        state = ActorState.RUNNING;
                        try {
                            monitor.resumed();
                        } catch(Throwable ex) {
                            throw new ActorException("Monitor failed.", ex);
                        }
                    }
                    break;
                case STOPPED:
                    sender.stop(new ActorException("Actor stopped."));
                    break;
                default:
                    throw new ActorException("Unexpected state " + state);
            }

            final Object returnValue;
            try {
                method.setAccessible(true);
                returnValue = method.invoke(impl, args);
                if(result != null) {
                    if(returnValue == null) {
                        result.apply(null, new NullPointerException());
                    } else {
                        ((IFuture<?>) returnValue).whenComplete((r, ex) -> result.apply(r, ex));
                    }
                }
            } catch(Throwable ex) {
                if(result != null && Arrays.asList(method.getExceptionTypes()).stream()
                        .anyMatch(cls -> cls.isAssignableFrom(ex.getClass()))) {
                    result.apply(null, ex);
                    return;
                } else {
                    throw new ActorException("Dispatch failed.", ex);
                }
            }

            try {
                monitor.delivered(sender, tags(method));
            } catch(Throwable ex) {
                throw new ActorException("Monitor failed.", ex);
            }
        });
    }

    @SuppressWarnings("rawtypes") private void _return(final IActorInternal<?> sender, final Method method,
            ICompletable completable, Object value, Throwable ex) {
        put(() -> {
            switch(state) {
                case INITIAL:
                    throw new ActorException("Cannot return on actor that was not started.");
                case RUNNING:
                    break;
                case WAITING:
                    if(isPrimaryMessage(method)) {
                        state = ActorState.RUNNING;
                        try {
                            monitor.resumed();
                        } catch(Throwable ex2) {
                            throw new ActorException("Monitor failed.", ex2);
                        }
                    }
                    break;
                case STOPPED:
                    sender.stop(new ActorException("Message return failed. Actor stopped."));
                    break;
                default:
                    throw new ActorException("Unexpected state " + state);
            }

            try {
                completable.complete(value, ex);
            } catch(Throwable ex2) {
                throw new ActorException("Return failed.", ex2);
            }

            try {
                monitor.delivered(sender, tags(method));
            } catch(Throwable ex2) {
                throw new ActorException("Monitor failed.", ex2);
            }
        });
    }

    @Override public void stop(Throwable ex) {
        final IActorInternal<?> sender = ActorThreadLocals.current.get();
        put(() -> {
            doStop(ex);
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Internal messaging (used in Actor{,System})
    ///////////////////////////////////////////////////////////////////////////

    private volatile T async;

    @Override public T async() {
        T result = async;
        if(async == null) {
            async = newAsync();
        }
        return result;
    }

    @SuppressWarnings({ "unchecked" }) private T newAsync() {
        return (T) Proxy.newProxyInstance(this.type.type().getClassLoader(), new Class[] { this.type.type() },
                (proxy, method, args) -> {
                    // WARNING This runs on the sender's thread!

                    if(method.getDeclaringClass().equals(Object.class)) {
                        return method.invoke(this, args);
                    }
                    final boolean primary = isPrimaryMessage(method);

                    final IActorInternal<?> sender = ActorThreadLocals.current.get();

                    final Class<?> returnType = method.getReturnType();
                    final IFuture<?> returnValue;

                    if(Void.TYPE.isAssignableFrom(returnType)) {
                        invoke(sender, method, args, null);
                        returnValue = null;
                    } else if(IFuture.class.isAssignableFrom(returnType)) {
                        final ICompletableFuture<?> result = new CompletableFuture<>();
                        invoke(sender, method, args, (r, ex) -> _return(sender, method, result, r, ex));
                        returnValue = result;
                    } else {
                        logger.error("Unsupported method called: {}", method);
                        throw new IllegalStateException("Unsupported method called: " + method);
                    }

                    if(primary) {
                        final Set<String> tags = tags(method);
                        monitor.sent(this, tags);
                    }

                    return returnValue;
                });
    }

    private Set<String> tags(Method method) {
        return Optional.ofNullable(method.getAnnotation(MessageTags.class)).map(tags -> CapsuleUtil.toSet(tags.value()))
                .orElse(CapsuleUtil.immutableSet());
    }

    private boolean isPrimaryMessage(Method method) {
        return method.getAnnotation(ControlMessage.class) == null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Messaging used by actor implementation (defined in IActor)
    ///////////////////////////////////////////////////////////////////////////

    @Override public <U> U async(IActorRef<U> receiver) {
        return context.async(receiver);
    }

    @Override public T local() {
        return async;
    }

    @Override public IActorRef<?> sender() {
        return sender.get();
    }

    @SuppressWarnings("unchecked") @Override public <U> IActorRef<U> sender(TypeTag<U> type) {
        return (IActorRef<U>) sender.get();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Sub actors
    ///////////////////////////////////////////////////////////////////////////

    @Override public <U> IActor<U> add(String id, TypeTag<U> type, Function1<IActor<U>, U> supplier) {
        final IActorImpl<U> actor = context.add(id, type, supplier);
        children.add(actor);
        actor.start(supplier);
        return actor;
    }

    ///////////////////////////////////////////////////////////////////////////
    // toString
    ///////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return "actor:" + id;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Thread correctness
    ///////////////////////////////////////////////////////////////////////////

    @Override public void assertOnActorThread() {
        if(thread == null) {
            logger.error("Actor {} is not running.", this);
            throw new IllegalStateException("Actor " + this + " is not running.");
        } else {
            final IActorInternal<?> current = ActorThreadLocals.current.get();
            if(thread != Thread.currentThread()) {
                logger.error("Actor {} is running on a different thread. (This thread is actor {}).", this, current);
                throw new IllegalStateException(
                        "Actor " + this + " is running on a different thread. (This thread is actor " + current + ").");
            } else if(!current.equals(this)) {
                logger.error("Actor {} is running, but thread and current are inconsistent.", this);
                throw new IllegalStateException(
                        "Actor " + this + " is running, but thread and current are inconsistent.");
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Stats
    ///////////////////////////////////////////////////////////////////////////

    @Override public IActorStats stats() {
        return stats;
    }

    private static class Stats implements IActorStats {

        private int suspended = 0;
        private int preempted = 0;
        private int rescheduled = 0;
        private int messages = 0;
        private int maxPendingMessages = 0;
        private int maxPendingMessagesOnActivate = 0;

        @Override public Iterable<String> csvHeaders() {
            return ImmutableList.of("suspended", "preempted", "rescheduled", "messages", "maxPendingMessages",
                    "maxPendingMessagesOnActivate");
        }

        @Override public Iterable<String> csvRow() {
            return ImmutableList.of(Integer.toString(suspended), Integer.toString(preempted),
                    Integer.toString(rescheduled), Integer.toString(messages), Integer.toString(maxPendingMessages),
                    Integer.toString(maxPendingMessagesOnActivate));
        }

        @Override public String toString() {
            return "ActorStats{messages=" + messages + ",maxPendingMessages=" + maxPendingMessages
                    + ",maxPendingMessagesOnActivate=" + maxPendingMessagesOnActivate + ",suspended=" + suspended
                    + ",preempted=" + preempted + ",rescheduled=" + rescheduled + "}";
        }

    }

}
