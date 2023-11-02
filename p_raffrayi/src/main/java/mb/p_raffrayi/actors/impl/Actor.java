package mb.p_raffrayi.actors.impl;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.Action2;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.ICompletable;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorMonitor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.actors.IActorStats;
import mb.p_raffrayi.actors.TypeTag;

class Actor<T> implements IActorImpl<T>, Runnable {

    private static final ILogger logger = LoggerUtils.logger(Actor.class);

    private final IActorContext context;
    private final String id;
    private final TypeTag<T> type;
    private final IActorInternal<?> parent;
    private final Set<IActorInternal<?>> children;

    private final AtomicBoolean running;
    private final AtomicReference<Runnable> scheduledTask;

    private volatile ActorState state;
    private final AtomicInteger priority;
    private final Deque<Message> messages;

    private T impl;
    private @Nullable IActorMonitor monitor;
    private @Nullable Throwable stopCause;

    private final int hashCode;

    private volatile Thread thread = null;
    private static final ThreadLocal<IActorInternal<?>> current = ThreadLocal.withInitial(() -> {
        final IllegalStateException ex = new IllegalStateException("Cannot get current actor.");
        logger.error("Cannot get current actor.", ex);
        throw ex;
    });


    private static final ThreadLocal<IActorRef<?>> sender = ThreadLocal.withInitial(() -> {
        logger.error("Cannot get sender. Not in message processing context?");
        throw new IllegalStateException("Cannot get sender. Not in message processing context?");
    });

    private Stats stats = new Stats();

    Actor(IActorContext context, IActorInternal<?> parent, String id, TypeTag<T> type) {
        this.context = context;
        this.id = id;
        this.type = type;
        this.parent = parent;
        this.children = new HashSet<>();

        this.running = new AtomicBoolean();
        this.scheduledTask = new AtomicReference<>();

        this.state = ActorState.INITIAL;
        this.priority = new AtomicInteger(0);
        this.messages = new ConcurrentLinkedDeque<>();

        this.hashCode = super.hashCode();
    }

    @Override public String id() {
        return id;
    }

    @Override public int hashCode() {
        return hashCode;
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

                final Message message = messages.poll();
                if(message != null) {

                    stats.messages += 1;
                    this.priority.decrementAndGet();
                    try {
                        message.dispatch();
                    } catch(Throwable ex) {
                        doStop(ex);
                    }

                    if(!messages.isEmpty() && context.scheduler().preempt(priority.get())) {
                        finalizeThread();
                        context.scheduler().schedule(this, priority.get(), scheduledTask);
                        return;
                    }

                } else {

                    if(state.equals(ActorState.RUNNING)) {
                        logger.debug("suspend");
                        stats.suspended += 1;

                        state = ActorState.WAITING;
                        try {
                            if(monitor != null) {
                                monitor.suspended();
                            }
                        } catch(Throwable ex) {
                            doStop(new ActorException("Suspend monitor failed.", ex));
                        }
                    }

                    finalizeThread();

                    if(!running.compareAndSet(true, false)) {
                        throw new IllegalStateException("Unexpected state, should be running.");
                    }

                    if(!messages.isEmpty() && running.compareAndSet(false, true)) {
                        initThread();
                    } else {
                        return;
                    }

                }
            }
        } catch(Throwable ex) {
            logger.error("Internal error.", ex);
        }
    }

    private void initThread() {
        if(thread != null) {
            logger.error("Actor already running on another thread.");
            throw new IllegalStateException("Actor already running on another thread.");
        }
        thread = Thread.currentThread();
        current.set(this);
        LoggerUtils.setContextId(this.toString());
    }

    private void finalizeThread() {
        LoggerUtils.clearContextId();
        current.remove();
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

    ///////////////////////////////////////////////////////////////////////////
    // IActorInternal -- unsafe, called from other actors and system
    ///////////////////////////////////////////////////////////////////////////

    @Override public void _start(IActorInternal<?> sender, Function1<IActor<T>, ? extends T> supplier) {
        put(() -> doStart(sender, supplier));
    }

    private volatile T dynamicAsync;

    @Override public T _invokeDynamic() {
        T result = dynamicAsync;
        if(result == null) {
            result = newAsync(current::get);
            dynamicAsync = result;
        }
        return result;
    }

    @Override public T _invokeStatic(IActorInternal<?> sender) {
        return newAsync(() -> sender);
    }

    @SuppressWarnings({ "unchecked" }) private T newAsync(Function0<IActorInternal<?>> senderGetter) {
        return (T) Proxy.newProxyInstance(this.type.type().getClassLoader(), new Class[] { this.type.type() },
                (proxy, method, args) -> {
                    // WARNING This runs on the sender's thread!

                    if(method.getDeclaringClass().equals(Object.class)) {
                        return method.invoke(this, args);
                    }

                    if(method.getDeclaringClass().equals(IActorMonitor.class)) {
                        logger.error("Illegal async actor monitor method called: {}", method);
                        throw new IllegalStateException("Illegal async actor monitor method called: " + method);
                    }

                    final IActorInternal<?> sender = senderGetter.apply();

                    final Class<?> returnType = method.getReturnType();
                    final IFuture<?> returnValue;

                    if(Void.TYPE.isAssignableFrom(returnType)) {
                        final Method method1 = method;
                        final Object[] args1 = args;
                        put(() -> {
                            doInvoke(sender, method1, args1, (Action2<Object, Throwable>) null);
                        });
                        returnValue = null;
                    } else if(IFuture.class.isAssignableFrom(returnType)) {
                        final ICompletableFuture<?> result = new CompletableFuture<>();
                        final Method method1 = method;
                        final Object[] args1 = args;
                        final Action2<Object, Throwable> _return =
                                (r, ex) -> sender._return(this, method, result, r, ex);
                        ;
                        put(() -> {
                            doInvoke((IActorInternal<?>) sender, method1, args1, _return);
                        });
                        returnValue = result;
                    } else {
                        logger.error("Unsupported method called: {}", method);
                        throw new IllegalStateException("Unsupported method called: " + method);
                    }

                    return returnValue;
                });
    }

    @Override public void _return(IActorInternal<?> sender, Method method,
            @SuppressWarnings("rawtypes") ICompletable result, Object value, Throwable ex) {
        put(() -> doReturn(sender, method, result, value, ex));
    }

    @Override public void _stop(IActorInternal<?> sender, Throwable ex) {
        logger.debug("{} recieved _stop from {}", this, sender);
        put(() -> doStop(ex));
    }

    @Override public void _childStopped(IActorInternal<?> sender, Throwable ex) {
        logger.debug("{} recieved _childStopped from {}", this, sender);
        put(() -> doChildStopped(sender, ex));
    }


    ///////////////////////////////////////////////////////////////////////////
    // Handlers -- safe, called from actor thread only
    ///////////////////////////////////////////////////////////////////////////

    private void doStart(IActorInternal<?> sender, Function1<IActor<T>, ? extends T> supplier) throws ActorException {
        assertOnActorThread();

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
        }

        state = ActorState.RUNNING;

        try {
            if(monitor != null) {
                monitor.started();
            }
        } catch(Throwable ex) {
            throw new ActorException("Start monitor failed.", ex);
        }
    }

    private void doInvoke(final IActorInternal<?> sender, final Method method, final Object[] args,
            Action2<Object, Throwable> result) throws ActorException {
        assertOnActorThread();

        updateStateOnReceive(sender);

        logger.debug("{} invoke {} from {}", this, method.getName(), sender);

        final Object returnValue;
        try {
            method.setAccessible(true);
            try {
                Actor.sender.set(sender);
                returnValue = method.invoke(impl, args);
            } finally {
                Actor.sender.remove();
            }
            if(result != null) {
                if(returnValue == null) {
                    result.apply(null, new NullPointerException(this + " invoke " + method + " from " + sender + " returned null."));
                } else {
                    ((IFuture<?>) returnValue).whenComplete((r, ex) -> result.apply(r, ex));
                }
            }
        } catch(Throwable ex) {
            throw new ActorException("Dispatch " + this + " invoke " + method + " from " + sender + " failed.", ex);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" }) private void doReturn(final IActorInternal<?> sender, Method method,
            ICompletable completable, Object value, Throwable ex) throws ActorException {
        assertOnActorThread();

        updateStateOnReceive(sender);

        logger.debug("{} return {} from {}", this, method.getName(), sender);

        try {
            try {
                Actor.sender.set(sender);
                completable.complete(value, ex);
            } finally {
                Actor.sender.remove();
            }
        } catch(Throwable ex2) {
            throw new ActorException("Return " + value + "/" + ex + "from " + this + " invoke " + method + " to " + sender + " failed.", ex2);
        }
    }

    private void updateStateOnReceive(final IActorInternal<?> sender) throws ActorException {
        switch(state) {
            case INITIAL:
                throw new ActorException("Cannot invoke method on actor that was not started.");
            case RUNNING:
                break;
            case WAITING:
                state = ActorState.RUNNING;
                try {
                    if(monitor != null) {
                        monitor.resumed();
                    }
                } catch(Throwable ex) {
                    throw new ActorException("Resume monitor failed.", ex);
                }
                break;
            case STOPPING:
            case STOPPED:
                Throwable ex2 = stopCause;
                if(ex2 == null || !(ex2 instanceof InterruptedException)) {
                    ex2 = new ActorException("Receiving actor stopping.", ex2);
                }
                sender._stop(this, ex2);
                break;
            default:
                throw new ActorException("Unexpected state " + state);
        }
    }

    private void doStop(Throwable ex) {
        assertOnActorThread();

        switch(state) {
            case INITIAL:
            case RUNNING:
            case WAITING:
                if(ex != null) {
                    if(ex instanceof InterruptedException) {
                        logger.debug("{} interrupted", this);
                    } else {
                        logger.error("{} failed", ex, this);
                    }
                }

                Throwable ex2 = ex;
                if(ex2 != null && !(ex2 instanceof InterruptedException)) {
                    ex2 = new ActorException("Actor " + this + " failed", ex);
                }

                state = ActorState.STOPPING;
                stopCause = ex2;
                for(IActorInternal<?> child : children) {
                    child._stop(this, ex2);
                }
                stopIfNoMoreChildren();
                break;
            case STOPPING:
            case STOPPED:
                break;
            default:
                throw new IllegalStateException("Unexpected state " + state);
        }
    }

    private void doChildStopped(IActorInternal<?> sender, Throwable ex) throws ActorException {
        assertOnActorThread();

        if(!children.remove(sender)) {
            throw new ActorException("Stopped actor " + sender + " is not a child of actor " + this);
        }

        Throwable ex2 = ex;
        if(ex2 != null && !(ex2 instanceof InterruptedException)) {
            ex2 = new ActorException("Child " + sender + " of " + this + " failed", ex);
        }

        switch(state) {
            case INITIAL:
                logger.error("Child {} stopped before parent {} started.", sender, this);
                throw new IllegalStateException("Child " + sender + " stopped before parent " + this + " started.");
            case RUNNING:
            case WAITING:
                doStop(ex2);
                stopIfNoMoreChildren(); // FIXME necessary?
                return;
            case STOPPING:
                stopIfNoMoreChildren();
                return;
            case STOPPED:
                logger.error("Child {} stopped after parent {} stopped.", sender, this);
                throw new IllegalStateException("Child " + sender + " stopped after parent " + this + " stopped.");
            default:
                throw new IllegalStateException("Unexpected state " + state);
        }
    }

    private void stopIfNoMoreChildren() {
        if(!state.equals(ActorState.STOPPING)) {
            return;
        }
        if(!children.isEmpty()) {
            return;
        }
        state = ActorState.STOPPED;
        try {
            if(monitor != null) {
                monitor.stopped(stopCause);
            }
        } catch(Throwable ex2) {
            logger.error("Stop monitor failed.", ex2);
        }
        parent._childStopped(this, stopCause);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IActor -- safe, called from implementation object
    ///////////////////////////////////////////////////////////////////////////

    @Override public <U> IActorRef<U> add(String id, TypeTag<U> type, Function1<IActor<U>, U> supplier) {
        final IActorImpl<U> actor = context.add(this, id, type);
        children.add(actor);
        actor._start(this, supplier);
        return actor;
    }

    @Override public <U> U async(IActorRef<U> receiver) {
        return context.async(receiver);
    }

    @Override public T local() {
        return _invokeDynamic();
    }

    @Override public <U> IFuture<U> schedule(IFuture<U> future) {
        final ICompletableFuture<U> scheduled = new CompletableFuture<>();
        future.whenComplete((r, ex) -> put(() -> scheduled.complete(r, ex)));
        return scheduled;
    }

    @Override public <U> void complete(ICompletable<U> completable, U result, Throwable ex) {
        put(() -> completable.complete(result, ex));
    }

    @Override public IActorRef<?> sender() {
        return sender.get();
    }

    @SuppressWarnings("unchecked") @Override public <U> IActorRef<U>
            sender(@SuppressWarnings("unused") TypeTag<U> type) {
        return (IActorRef<U>) sender.get();
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
            final IActorInternal<?> current = Actor.current.get();
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

    private static class Stats implements IActorStats, Serializable {

        private static final long serialVersionUID = 42L;

        private int suspended = 0;
        private int preempted = 0;
        private int rescheduled = 0;
        private int messages = 0;
        private int maxPendingMessages = 0;
        private int maxPendingMessagesOnActivate = 0;

        @Override public Collection<String> csvHeaders() {
            return ImList.Immutable.of("suspended", "preempted", "rescheduled", "messages", "maxPendingMessages",
                    "maxPendingMessagesOnActivate");
        }

        @Override public Collection<String> csvRow() {
            return ImList.Immutable.of(Integer.toString(suspended), Integer.toString(preempted),
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
