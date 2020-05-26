package mb.statix.solver.concurrent;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;

import io.usethesource.capsule.Set;
import mb.statix.solver.concurrent.messages.AddEdge;
import mb.statix.solver.concurrent.messages.ClientMessage;
import mb.statix.solver.concurrent.messages.CloseEdge;
import mb.statix.solver.concurrent.messages.CoordinatorMessage;
import mb.statix.solver.concurrent.messages.DeadLock;
import mb.statix.solver.concurrent.messages.Done;
import mb.statix.solver.concurrent.messages.FreshScope;
import mb.statix.solver.concurrent.messages.Query;
import mb.statix.solver.concurrent.messages.QueryAnswer;
import mb.statix.solver.concurrent.messages.ScopeAnswer;
import mb.statix.solver.concurrent.messages.Start;
import mb.statix.solver.concurrent.messages.StartAnswer;
import mb.statix.solver.concurrent.messages.Suspend;

public abstract class AbstractTypeChecker<S, L, D> implements ClientMessage.Cases<S, L, D> {

    // local state

    private enum State {
        INIT, ACTIVE, WAITING, DONE, DEADLOCKED
    }

    private final BlockingQueue<ClientMessage<S, L, D>> inbox = Queues.newLinkedBlockingDeque();

    private final Coordinator<S, L, D> coordinator;
    private final CompletableFuture<Object> pendingResult = new CompletableFuture<>();
    private State state;

    private long lastMessageId = 0;

    private final Map<Long, CompletableFuture<S>> pendingScopes = Maps.newHashMap();
    private final Map<Long, CompletableFuture<Set.Immutable<Object>>> pendingAnswers = Maps.newHashMap();

    public AbstractTypeChecker(String resource, Coordinator<S, L, D> coordinator) {
        this.coordinator = coordinator;
        this.state = State.INIT;
        coordinator.register(resource, this);
    }

    public CompletableFuture<Object> run(ExecutorService executor) {
        executor.submit(() -> {
            try {
                run();
            } catch(InterruptedException e) {
                pendingResult.completeExceptionally(e);
            }
        });
        return pendingResult;
    }

    final void post(ClientMessage<S, L, D> message) {
        inbox.add(message);
    }

    final long post(CoordinatorMessage<S, L, D> message) {
        final long messageId = ++lastMessageId;
        coordinator.post(message.withId(messageId));
        return messageId;
    }

    // message handling

    final void run() throws InterruptedException {
        while(!inState(State.DONE, State.DEADLOCKED)) {
            if(inbox.isEmpty()) {
                state = State.WAITING;
                post(Suspend.of(this));
            }
            final ClientMessage<S, L, D> message = inbox.take();
            message.match(this);
        }
    }

    @Override public final void on(Start<S, L, D> message) throws InterruptedException {
        if(!inState(State.INIT)) {
            throw new IllegalStateException("Expected state INIT, got " + state.toString());
        }
    }

    @Override public final void on(ScopeAnswer<S, L, D> message) throws InterruptedException {
        if(!inState(State.ACTIVE, State.WAITING)) {
            throw new IllegalStateException("Expected state WAITING, got " + state.toString());
        }
        state = State.ACTIVE;
        if(!pendingScopes.containsKey(message.requestId())) {
            throw new IllegalStateException("Missing pending answer for query " + message.requestId());
        }
        pendingScopes.remove(message.requestId()).complete(message.scope());
    }

    @Override public final void on(QueryAnswer<S, L, D> message) throws InterruptedException {
        if(!inState(State.ACTIVE, State.WAITING)) {
            throw new IllegalStateException("Expected state WAITING, got " + state.toString());
        }
        state = State.ACTIVE;
        if(!pendingAnswers.containsKey(message.requestId())) {
            throw new IllegalStateException("Missing pending answer for query " + message.requestId());
        }
        pendingAnswers.remove(message.requestId()).complete(message.paths());
    }

    @Override public final void on(DeadLock<S, L, D> message) throws InterruptedException {
        if(!inState(State.ACTIVE, State.WAITING)) {
            throw new IllegalStateException("Expected state WAITING, got " + state.toString());
        }
        state = State.DEADLOCKED;
        fail();
    }

    private boolean inState(State... states) {
        for(State state : states) {
            if(state.equals(this.state)) {
                return true;
            }
        }
        return false;
    }


    // required interface, implemented by client

    public abstract void start(S root);

    public abstract void fail();


    // provided interface, called by client

    public final void started(Set.Immutable<L> labels) {
        if(!inState(State.INIT)) {
            throw new IllegalStateException("Expected state ACTIVE, got " + state.toString());
        }
        state = State.ACTIVE;
        coordinator.post(StartAnswer.of(this, labels));
    }

    public final CompletableFuture<S> freshScope(D datum, java.util.Set<L> labels) {
        if(!inState(State.ACTIVE)) {
            throw new IllegalStateException("Expected state ACTIVE, got " + state.toString());
        }
        final long messageId = post(FreshScope.of(this, datum, labels));
        final CompletableFuture<S> pendingScope = new CompletableFuture<>();
        pendingScopes.put(messageId, pendingScope);
        return pendingScope;
    }

    public final void addEdge(S source, L label, S target) {
        if(!inState(State.ACTIVE)) {
            throw new IllegalStateException("Expected state ACTIVE, got " + state.toString());
        }
        post(AddEdge.of(this, source, label, target));
    }

    public final void closeEdge(S source, L label) {
        if(!inState(State.ACTIVE)) {
            throw new IllegalStateException("Expected state ACTIVE, got " + state.toString());
        }
        post(CloseEdge.of(this, source, label));
    }

    public final CompletableFuture<Set.Immutable<Object>> query(S scope) {
        if(!inState(State.ACTIVE)) {
            throw new IllegalStateException("Expected state ACTIVE, got " + state.toString());
        }
        final long messageId = post(Query.of(this, scope));
        final CompletableFuture<Set.Immutable<Object>> pendingAnswer = new CompletableFuture<>();
        pendingAnswers.put(messageId, pendingAnswer);
        return pendingAnswer;
    }

    public final void done(Object result) {
        if(!inState(State.ACTIVE)) {
            throw new IllegalStateException("Expected state ACTIVE, got " + state.toString());
        }
        post(Done.of(this));
        state = State.DONE;
        pendingResult.complete(result);
    }


}