package mb.statix.concurrent._attic.messages;

import java.util.Set;

import org.immutables.value.Value;

import mb.statix.scopegraph.path.IResolutionPath;

public abstract class ClientMessage<S, L, D> {

    public abstract void match(Cases<S, L, D> cases) throws InterruptedException;

    public interface Cases<S, L, D> {

        void on(Start<S, L, D> message) throws InterruptedException;

        void on(ScopeAnswer<S, L, D> message) throws InterruptedException;

        void on(QueryAnswer<S, L, D> message) throws InterruptedException;

        void on(QueryFailed<S, L, D> message) throws InterruptedException;

    }

    @Value.Immutable
    public static abstract class AStart<S, L, D> extends ClientMessage<S, L, D> {

        @Value.Parameter public abstract S root();

        @Override public void match(Cases<S, L, D> cases) throws InterruptedException {
            cases.on((Start<S, L, D>) this);
        }

    }

    @Value.Immutable
    public static abstract class AScopeAnswer<S, L, D> extends ClientMessage<S, L, D> {

        @Value.Parameter public abstract long requestId();

        @Value.Parameter public abstract S scope();

        @Override public void match(Cases<S, L, D> cases) throws InterruptedException {
            cases.on((ScopeAnswer<S, L, D>) this);
        }

    }

    @Value.Immutable
    public static abstract class AQueryAnswer<S, L, D> extends ClientMessage<S, L, D> {

        @Value.Parameter public abstract long requestId();

        @Value.Parameter public abstract Set<IResolutionPath<S, L, D>> paths();

        @Override public void match(Cases<S, L, D> cases) throws InterruptedException {
            cases.on((QueryAnswer<S, L, D>) this);
        }

    }

    @Value.Immutable
    public static abstract class AQueryFailed<S, L, D> extends ClientMessage<S, L, D> {

        @Value.Parameter public abstract long requestId();

        @Value.Parameter public abstract Throwable cause();

        @Override public void match(Cases<S, L, D> cases) throws InterruptedException {
            cases.on((QueryFailed<S, L, D>) this);
        }

    }

}