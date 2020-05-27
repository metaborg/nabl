package mb.statix.solver.concurrent;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

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

public abstract class AbstractTypeChecker<S, L, D, R> implements ClientMessage.Cases<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(AbstractTypeChecker.class);

    // local state

    private final String resource;

    private final BlockingQueue<ClientMessage<S, L, D>> inbox = Queues.newLinkedBlockingDeque();

    private final Coordinator<S, L, D> coordinator;
    private final CompletableFuture<R> pendingResult = new CompletableFuture<>();
    private State state;

    private int clock = 0;
    private int lastMessageId = 0;

    private final Map<Long, CompletableFuture<S>> pendingScopes = Maps.newHashMap();
    private final Map<Long, CompletableFuture<Set.Immutable<Object>>> pendingAnswers = Maps.newHashMap();

    public AbstractTypeChecker(String resource, Coordinator<S, L, D> coordinator) {
        this.resource = resource;
        this.coordinator = coordinator;
        setState(State.INIT);
        coordinator.register(this);
    }

    public String resource() {
        return resource;
    }

    private void setState(State state) {
        logger.info("client {} is in state {}", this, state);
        this.state = state;
    }

    public CompletableFuture<R> run(ExecutorService executor) {
        executor.submit(() -> {
            try {
                run();
            } catch(Throwable e) {
                pendingResult.completeExceptionally(e);
            }
        });
        return pendingResult;
    }

    final void receive(ClientMessage<S, L, D> message) {
        logger.info("client {} received {}", this, message);
        inbox.add(message);
    }

    private final long post(CoordinatorMessage<S, L, D> message) {
        final int messageId = ++lastMessageId;
        message = message.withClient(this).withClock(clock).withId(messageId);
        logger.info("post {} to coordinator", message);
        coordinator.receive(message);
        return messageId;
    }

    // message handling

    final void run() throws InterruptedException {
        while(!inState(State.DONE, State.DEADLOCKED)) {
            if(inbox.isEmpty()) {
                logger.info("client {} suspended", this);
                post(Suspend.<S, L, D>builder().build());
            }
            final ClientMessage<S, L, D> message = inbox.take();
            clock += 1; // increase count of messages received from coordinator
            logger.info("client {} processing {}", this, message);
            message.match(this);
        }
        logger.info("client {} finished.", this);
    }

    @Override public final void on(Start<S, L, D> message) throws InterruptedException {
        if(!inState(State.INIT)) {
            throw new IllegalStateException("Expected state INIT, got " + state.toString());
        }
        start(message.root());
    }

    @Override public final void on(ScopeAnswer<S, L, D> message) throws InterruptedException {
        if(!inState(State.ACTIVE)) {
            throw new IllegalStateException("Expected state WAITING, got " + state.toString());
        }
        if(!pendingScopes.containsKey(message.requestId())) {
            throw new IllegalStateException("Missing pending answer for query " + message.requestId());
        }
        pendingScopes.remove(message.requestId()).complete(message.scope());
    }

    @Override public final void on(QueryAnswer<S, L, D> message) throws InterruptedException {
        if(!inState(State.ACTIVE)) {
            throw new IllegalStateException("Expected state WAITING, got " + state.toString());
        }
        if(!pendingAnswers.containsKey(message.requestId())) {
            throw new IllegalStateException("Missing pending answer for query " + message.requestId());
        }
        pendingAnswers.remove(message.requestId()).complete(message.paths());
    }

    @Override public final void on(DeadLock<S, L, D> message) throws InterruptedException {
        if(!inState(State.ACTIVE)) {
            throw new IllegalStateException("Expected state WAITING, got " + state.toString());
        }
        setState(State.DEADLOCKED);
        pendingResult.completeExceptionally(new Exception("deadlocked"));
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
        setState(State.ACTIVE);
        post(StartAnswer.of(labels));
    }

    public final CompletableFuture<S> freshScope(D datum, java.util.Set<L> labels) {
        if(!inState(State.ACTIVE)) {
            throw new IllegalStateException("Expected state ACTIVE, got " + state.toString());
        }
        final long messageId = post(FreshScope.of(datum, labels));
        final CompletableFuture<S> pendingScope = new CompletableFuture<>();
        pendingScopes.put(messageId, pendingScope);
        return pendingScope;
    }

    public final void addEdge(S source, L label, S target) {
        if(!inState(State.ACTIVE)) {
            throw new IllegalStateException("Expected state ACTIVE, got " + state.toString());
        }
        post(AddEdge.of(source, label, target));
    }

    public final void closeEdge(S source, L label) {
        if(!inState(State.ACTIVE)) {
            throw new IllegalStateException("Expected state ACTIVE, got " + state.toString());
        }
        post(CloseEdge.of(source, label));
    }

    public final CompletableFuture<Set.Immutable<Object>> query(S scope) {
        if(!inState(State.ACTIVE)) {
            throw new IllegalStateException("Expected state ACTIVE, got " + state.toString());
        }
        final long messageId = post(Query.of(scope));
        final CompletableFuture<Set.Immutable<Object>> pendingAnswer = new CompletableFuture<>();
        pendingAnswers.put(messageId, pendingAnswer);
        return pendingAnswer;
    }

    public final void done(R result) {
        if(!inState(State.ACTIVE)) {
            throw new IllegalStateException("Expected state ACTIVE, got " + state.toString());
        }
        setState(State.DONE);
        post(Done.<S, L, D>builder().build());
        pendingResult.complete(result);
    }

    @Override public String toString() {
        return "TypeChecker[" + resource + "]";
    }

}