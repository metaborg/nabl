package mb.statix.solver.concurrent.util;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.BlockingQueue;

import com.google.common.collect.Queues;

public class Async<T> {

    private final T receiver;
    private final T async;
    private final BlockingQueue<Invocation> invocations;

    @SuppressWarnings("unchecked") private Async(Class<T> receiverClass, T receiver) {
        this.receiver = receiver;
        this.invocations = Queues.newLinkedBlockingDeque();
        validate();
        this.async = (T) Proxy.newProxyInstance(receiver.getClass().getClassLoader(),
                new Class[] { receiver.getClass() }, (proxy, method, args) -> {
                    invocations.put(new Invocation(method, args));
                    return null;
                });
    }

    private void validate() {
        for(Method method : receiver.getClass().getMethods()) {
            if(!method.getReturnType().equals(Void.TYPE)) {
                throw new IllegalArgumentException(
                        "Cannot implement Async for non-void return type of method " + method);
            }
            if(method.getExceptionTypes().length > 0) {
                throw new IllegalArgumentException("Cannot implement Async for checked exceptions of method " + method);
            }
        }
    }

    /**
     * Return if there are pending invocations.
     */
    public boolean isEmpty() {
        return invocations.isEmpty();
    }

    /**
     * Await the next invocation.
     */
    public void await() throws InterruptedException {
        invocations.take().invoke();
    }

    /**
     * Get an async interface to the receiver, that can be called non-blocking.
     */
    public T async() {
        return async;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private class Invocation {

        public final Method method;
        public final Object[] args;

        public Invocation(Method method, Object[] args) {
            this.method = method;
            this.args = args;
        }

        public void invoke() {
            try {
                final Object returnValue = method.invoke(receiver, args);
//                if(result != null) {
//                    result.complete(returnValue);
//                }
            } catch(ReflectiveOperationException | IllegalArgumentException ex) {
//                if(result != null) {
//                    result.completeExceptionally(ex);
//                }
            }
        }

    }

}