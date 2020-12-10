package mb.statix.concurrent.actors.futures;

import org.metaborg.util.functions.CheckedAction1;
import org.metaborg.util.functions.CheckedAction2;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.CheckedFunction2;

@SuppressWarnings("unchecked")
public class CompletableFuture<T> implements ICompletableFuture<T> {

    private static enum State {
        OPEN, VALUE, EXCEPTION
    }

    private volatile State state = State.OPEN;
    private volatile Object data;

    public CompletableFuture() {
    }

    /////////////////////////////////////////////////////////////////////
    // ICompletable
    /////////////////////////////////////////////////////////////////////

    @Override public void complete(T value, Throwable ex) {
        Handler<T> handler = null;
        synchronized(this) {
            if(state.equals(State.OPEN)) {
                handler = (Handler<T>) data;
                if(ex != null) {
                    state = State.EXCEPTION;
                    data = ex;
                } else {
                    state = State.VALUE;
                    data = value;
                }
            }
        }
        while(handler != null) {
            handler.handle(value, ex);
            handler = handler.parent;
        }
    }

    /////////////////////////////////////////////////////////////////////
    // IFuture
    /////////////////////////////////////////////////////////////////////

    @Override public <U> IFuture<U> compose(
            CheckedFunction2<? super T, Throwable, ? extends IFuture<? extends U>, ? extends Throwable> handler) {
        State handle = state;
        if(handle.equals(State.OPEN)) {
            synchronized(this) {
                if(state.equals(State.OPEN)) {
                    final ICompletableFuture<U> future = new CompletableFuture<>();
                    data = (Handler<T>) new Handler<T>((Handler<T>) data) {

                        @Override public void handle(T r, Throwable ex) {
                            try {
                                handler.apply(r, ex).whenComplete(future::complete);
                            } catch(Throwable ex2) {
                                future.completeExceptionally(ex2);
                            }
                        }

                    };
                    return future;
                } else {
                    handle = state;
                }
            }
        }
        try {
            switch(handle) {
                case VALUE:
                    return (IFuture<U>) handler.apply((T) data, null);
                case EXCEPTION:
                    return (IFuture<U>) handler.apply(null, (Throwable) data);
                default:
                    throw new IllegalStateException();
            }
        } catch(Throwable ex) {
            return new CompletedExceptionallyFuture<>(ex);
        }
    }

    @Override public <U> IFuture<U> thenApply(CheckedFunction1<? super T, ? extends U, ? extends Throwable> handler) {
        State handle = state;
        if(handle.equals(State.OPEN)) {
            synchronized(this) {
                if(state.equals(State.OPEN)) {
                    final ICompletableFuture<U> future = new CompletableFuture<>();
                    data = (Handler<T>) new Handler<T>((Handler<T>) data) {

                        @Override public void handle(T r, Throwable ex) {
                            if(ex != null) {
                                future.completeExceptionally(ex);
                            } else {
                                try {
                                    future.complete(handler.apply(r));
                                } catch(Throwable ex2) {
                                    future.completeExceptionally(ex2);
                                }
                            }
                        }

                    };
                    return future;
                } else {
                    handle = state;
                }
            }
        }
        switch(handle) {
            case VALUE:
                try {
                    return new CompletedFuture<>(handler.apply((T) data));
                } catch(Throwable ex) {
                    return new CompletedExceptionallyFuture<>(ex);
                }
            case EXCEPTION:
                return new CompletedExceptionallyFuture<>((Throwable) data);
            default:
                throw new IllegalStateException();
        }
    }

    @Override public IFuture<Void> thenAccept(CheckedAction1<? super T, ? extends Throwable> handler) {
        State handle = state;
        if(handle.equals(State.OPEN)) {
            synchronized(this) {
                if(state.equals(State.OPEN)) {
                    final ICompletableFuture<Void> future = new CompletableFuture<>();
                    data = (Handler<T>) new Handler<T>((Handler<T>) data) {

                        @Override public void handle(T r, Throwable ex) {
                            if(ex != null) {
                                future.completeExceptionally(ex);
                            } else {
                                try {
                                    handler.apply(r);
                                    future.complete(null);
                                } catch(Throwable ex2) {
                                    future.completeExceptionally(ex2);
                                }
                            }
                        }

                    };
                    return future;
                } else {
                    handle = state;
                }
            }
        }
        switch(handle) {
            case VALUE:
                try {
                    handler.apply((T) data);
                    return new CompletedFuture<>(null);
                } catch(Throwable ex) {
                    return new CompletedExceptionallyFuture<>(ex);
                }
            case EXCEPTION:
                return new CompletedExceptionallyFuture<>((Throwable) data);
            default:
                throw new IllegalStateException();
        }
    }

    @Override public <U> IFuture<U>
            thenCompose(CheckedFunction1<? super T, ? extends IFuture<? extends U>, ? extends Throwable> handler) {
        State handle = state;
        if(handle.equals(State.OPEN)) {
            synchronized(this) {
                if(state.equals(State.OPEN)) {
                    final ICompletableFuture<U> future = new CompletableFuture<>();
                    data = (Handler<T>) new Handler<T>((Handler<T>) data) {

                        @Override public void handle(T r, Throwable ex) {
                            if(ex != null) {
                                future.completeExceptionally(ex);
                            } else {
                                try {
                                    handler.apply(r).whenComplete(future::complete);
                                } catch(Throwable ex2) {
                                    future.completeExceptionally(ex2);
                                }
                            }
                        }

                    };
                    return future;
                } else {
                    handle = state;
                }
            }
        }
        switch(handle) {
            case VALUE:
                try {
                    return (IFuture<U>) handler.apply((T) data);
                } catch(Throwable ex) {
                    return new CompletedExceptionallyFuture<>(ex);
                }
            case EXCEPTION:
                return new CompletedExceptionallyFuture<>((Throwable) data);
            default:
                throw new IllegalStateException();
        }
    }

    @Override public <U> IFuture<U>
            handle(CheckedFunction2<? super T, Throwable, ? extends U, ? extends Throwable> handler) {
        State handle = state;
        if(handle.equals(State.OPEN)) {
            synchronized(this) {
                if(state.equals(State.OPEN)) {
                    final ICompletableFuture<U> future = new CompletableFuture<>();
                    data = (Handler<T>) new Handler<T>((Handler<T>) data) {

                        @Override public void handle(T r, Throwable ex) {
                            try {
                                future.complete(handler.apply(r, ex));
                            } catch(Throwable ex2) {
                                future.completeExceptionally(ex2);
                            }
                        };

                    };
                    return future;
                } else {
                    handle = state;
                }
            }
        }
        try {
            switch(handle) {
                case VALUE:
                    return new CompletedFuture<>(handler.apply((T) data, null));
                case EXCEPTION:
                    return new CompletedFuture<>(handler.apply(null, (Throwable) data));
                default:
                    throw new IllegalStateException();
            }
        } catch(Throwable ex) {
            return new CompletedExceptionallyFuture<>(ex);
        }
    }

    @Override public IFuture<T> whenComplete(CheckedAction2<? super T, Throwable, ? extends Throwable> handler) {
        State handle = state;
        if(handle.equals(State.OPEN)) {
            synchronized(this) {
                if(state.equals(State.OPEN)) {
                    final ICompletableFuture<T> future = new CompletableFuture<>();
                    data = (Handler<T>) new Handler<T>((Handler<T>) data) {

                        @Override public void handle(T r, Throwable ex) {
                            try {
                                handler.apply(r, ex);
                                future.complete(r, ex);
                            } catch(Throwable ex2) {
                                future.completeExceptionally(ex2);
                            }
                        };

                    };
                    return future;
                } else {
                    handle = state;
                }
            }
        }
        try {
            switch(handle) {
                case VALUE:
                    handler.apply((T) data, null);
                    return new CompletedFuture<>((T) data);
                case EXCEPTION:
                    handler.apply(null, (Throwable) data);
                    return new CompletedExceptionallyFuture<>((Throwable) data);
                default:
                    throw new IllegalStateException();
            }
        } catch(Throwable ex) {
            return new CompletedExceptionallyFuture<>(ex);
        }
    }


    @Override public boolean isDone() {
        synchronized(this) {
            return !state.equals(State.OPEN);
        }
    }

    @Override public java.util.concurrent.CompletableFuture<T> asJavaCompletion() {
        State handle = null;
        synchronized(this) {
            if(state.equals(State.OPEN)) {
                final java.util.concurrent.CompletableFuture<T> future = new java.util.concurrent.CompletableFuture<>();
                data = (Handler<T>) new Handler<T>((Handler<T>) data) {

                    @Override public void handle(T r, Throwable ex) {
                        if(ex != null) {
                            future.completeExceptionally(ex);
                        } else {
                            future.complete(r);
                        }
                    };

                };
                return future;
            } else {
                handle = state;
            }
        }
        {
            final java.util.concurrent.CompletableFuture<T> future = new java.util.concurrent.CompletableFuture<>();
            switch(handle) {
                case VALUE:
                    future.complete((T) data);
                    break;
                case EXCEPTION:
                    future.completeExceptionally((Throwable) data);
                    break;
                default:
                    throw new IllegalStateException();
            }
            return future;
        }
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }

    private abstract static class Handler<T> {

        private final Handler<T> parent;

        public Handler(Handler<T> parent) {
            this.parent = parent;
        }

        public abstract void handle(T r, Throwable ex);

    }

    public static <T> IFuture<T> completedFuture(T value) {
        return new CompletedFuture<>(value);
    }

    public static <T> IFuture<T> completedExceptionally(Throwable ex) {
        return new CompletedExceptionallyFuture<>(ex);
    }

}