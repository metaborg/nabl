package mb.statix.solver.concurrent.messages;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import mb.statix.solver.concurrent.AbstractTypeChecker;

public abstract class CoordinatorMessage<S, L, D> {

    @Value.Default public @Nullable AbstractTypeChecker<S, L, D, ?> client() {
        return null;
    }

    @Value.Default public int clock() {
        return 0;
    }

    @Value.Default public int id() {
        return 0;
    }

    public abstract CoordinatorMessage<S, L, D> withClient(AbstractTypeChecker<S, L, D, ?> client);

    public abstract CoordinatorMessage<S, L, D> withClock(int clock);

    public abstract CoordinatorMessage<S, L, D> withId(int id);

    public abstract void match(Cases<S, L, D> cases) throws InterruptedException;

    public interface Cases<S, L, D> {

        void on(RootEdges<S, L, D> message) throws InterruptedException;

        void on(FreshScope<S, L, D> message) throws InterruptedException;

        void on(AddEdge<S, L, D> message) throws InterruptedException;

        void on(CloseEdge<S, L, D> message) throws InterruptedException;

        void on(Query<S, L, D> message) throws InterruptedException;

        void on(Suspend<S, L, D> message) throws InterruptedException;

        void on(Done<S, L, D> message) throws InterruptedException;

        void on(Failed<S, L, D> message) throws InterruptedException;

    }

    @Value.Immutable
    public static abstract class ARootEdges<S, L, D> extends CoordinatorMessage<S, L, D> {

        @Value.Parameter public abstract java.util.Set<L> labels();

        @Override public void match(Cases<S, L, D> cases) throws InterruptedException {
            cases.on((RootEdges<S, L, D>) this);
        }

    }

    @Value.Immutable
    public static abstract class AFreshScope<S, L, D> extends CoordinatorMessage<S, L, D> {

        @Value.Parameter public abstract D datum();

        @Value.Parameter public abstract java.util.Set<L> labels();

        @Override public void match(Cases<S, L, D> cases) throws InterruptedException {
            cases.on((FreshScope<S, L, D>) this);
        }

    }

    @Value.Immutable
    public static abstract class AAddEdge<S, L, D> extends CoordinatorMessage<S, L, D> {

        @Value.Parameter public abstract S source();

        @Value.Parameter public abstract L label();

        @Value.Parameter public abstract S target();

        @Override public void match(Cases<S, L, D> cases) throws InterruptedException {
            cases.on((AddEdge<S, L, D>) this);
        }

    }

    @Value.Immutable
    public static abstract class ACloseEdge<S, L, D> extends CoordinatorMessage<S, L, D> {

        @Value.Parameter public abstract S source();

        @Value.Parameter public abstract L label();

        @Override public void match(Cases<S, L, D> cases) throws InterruptedException {
            cases.on((CloseEdge<S, L, D>) this);
        }

    }

    @Value.Immutable
    public static abstract class AQuery<S, L, D> extends CoordinatorMessage<S, L, D> {

        @Value.Parameter public abstract S scope();

        @Override public void match(Cases<S, L, D> cases) throws InterruptedException {
            cases.on((Query<S, L, D>) this);
        }

    }

    @Value.Immutable
    public static abstract class ASuspend<S, L, D> extends CoordinatorMessage<S, L, D> {

        @Override public void match(Cases<S, L, D> cases) throws InterruptedException {
            cases.on((Suspend<S, L, D>) this);
        }

    }

    @Value.Immutable
    public static abstract class ADone<S, L, D> extends CoordinatorMessage<S, L, D> {

        @Override public void match(Cases<S, L, D> cases) throws InterruptedException {
            cases.on((Done<S, L, D>) this);
        }

    }

    @Value.Immutable
    public static abstract class AFailed<S, L, D> extends CoordinatorMessage<S, L, D> {

        @Value.Parameter public abstract Throwable cause();

        @Override public void match(Cases<S, L, D> cases) throws InterruptedException {
            cases.on((Failed<S, L, D>) this);
        }

    }

}