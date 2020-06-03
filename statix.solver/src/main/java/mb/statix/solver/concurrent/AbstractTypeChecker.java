package mb.statix.solver.concurrent;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;

import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.solver.concurrent.messages.AddEdge;
import mb.statix.solver.concurrent.messages.ClientMessage;
import mb.statix.solver.concurrent.messages.CloseEdge;
import mb.statix.solver.concurrent.messages.CoordinatorMessage;
import mb.statix.solver.concurrent.messages.DeadLock;
import mb.statix.solver.concurrent.messages.Done;
import mb.statix.solver.concurrent.messages.Failed;
import mb.statix.solver.concurrent.messages.FreshScope;
import mb.statix.solver.concurrent.messages.Query;
import mb.statix.solver.concurrent.messages.QueryAnswer;
import mb.statix.solver.concurrent.messages.RootEdges;
import mb.statix.solver.concurrent.messages.ScopeAnswer;
import mb.statix.solver.concurrent.messages.Start;
import mb.statix.solver.concurrent.messages.Suspend;

public abstract class AbstractTypeChecker<S, L, D, R>
        implements ClientMessage.Cases<S, L, D>, IScopeGraphFacade<S, L, D>, ITypeChecker<S, L, D, R> {

    private static final ILogger logger = LoggerUtils.logger(AbstractTypeChecker.class);

    // local state

    private final String resource;

    private final BlockingQueue<ClientMessage<S, L, D>> inbox = Queues.newLinkedBlockingDeque();
    private int clock = 0;
    private int lastMessageId = 0;

    private final Coordinator<S, L, D> coordinator;
    private final CompletableFuture<R> pendingResult = new CompletableFuture<>();
    private State state;
    private boolean waiting;

    private final Map<Long, CompletableFuture<S>> pendingScopes = Maps.newHashMap();
    private final Map<Long, CompletableFuture<java.util.Set<IResolutionPath<S, L, D>>>> pendingQueries =
            Maps.newHashMap();
    private final Multimap<S, L> openEdges = HashMultimap.create();

    public AbstractTypeChecker(String resource, Coordinator<S, L, D> coordinator) {
        this.resource = resource;
        this.coordinator = coordinator;
        setState(State.INIT);
        this.waiting = false;
        coordinator.register(this);
    }

    public final String resource() {
        return resource;
    }

    // required interface, implemented by client

    @Override public abstract CompletableFuture<R> run(S root) throws InterruptedException;

    // misc

    private void setState(State state) {
        logger.info("client {} is in state {}", this, state);
        this.state = state;
    }

    public final CompletableFuture<R> run(ExecutorService executor) {
        executor.submit(() -> {
            try {
                run();
            } catch(Throwable e) {
                failed(e);
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
        while(inState(State.INIT, State.ACTIVE)) {
            if(inbox.isEmpty()) {
                logger.info("client {} suspended: pending {} scopes, {} queries, {} open edges", this,
                        pendingScopes.size(), pendingQueries.size(), openEdges.size());
                logger.info("client {} open edges: {}", this, openEdges);
                post(Suspend.<S, L, D>builder().build());
                waiting = true;
            }
            final ClientMessage<S, L, D> message = inbox.take();
            if(waiting) {
                waiting = false;
            }
            clock += 1; // increase count of messages received from coordinator
            logger.info("client {} processing {}", this, message);
            message.match(this);
        }
        // TODO if failed, absorb all other messages?
        logger.info("client {} finished: pending {} scopes, {} queries", this, pendingScopes.size(),
                pendingQueries.size());
    }

    @Override public final void on(Start<S, L, D> message) throws InterruptedException {
        assertInState(State.INIT);
        run(message.root()).handle((r, ex) -> {
            if(ex != null) {
                failed(ex);
            } else {
                done(r);
            }
            return Unit.unit;
        });
        assertInState(State.ACTIVE, State.DONE, State.FAILED);
    }

    @Override public final void on(ScopeAnswer<S, L, D> message) throws InterruptedException {
        assertInState(State.ACTIVE);
        if(!pendingScopes.containsKey(message.requestId())) {
            throw new IllegalStateException("Missing pending answer for query " + message.requestId());
        }
        pendingScopes.remove(message.requestId()).complete(message.scope());
    }

    @Override public final void on(QueryAnswer<S, L, D> message) throws InterruptedException {
        assertInState(State.ACTIVE);
        if(!pendingQueries.containsKey(message.requestId())) {
            throw new IllegalStateException("Missing pending answer for query " + message.requestId());
        }
        pendingQueries.remove(message.requestId()).complete(message.paths());
    }

    @Override public final void on(DeadLock<S, L, D> message) throws InterruptedException {
        assertInState(State.ACTIVE);
        if(!pendingQueries.containsKey(message.requestId())) {
            throw new IllegalStateException("Missing pending answer for query " + message.requestId());
        }
        pendingQueries.remove(message.requestId()).completeExceptionally(new DeadLockedException());
    }

    private boolean inState(State... states) {
        for(State state : states) {
            if(state.equals(this.state)) {
                return true;
            }
        }
        return false;
    }

    private void assertInState(State... states) {
        if(!inState(states)) {
            throw new IllegalStateException("Expected state " + Arrays.toString(states) + ", got " + state.toString());
        }
    }

    private void assertEdgeOpen(S source, L label) {
        if(!openEdges.containsEntry(source, label)) {
            throw new IllegalArgumentException("Edge " + source + "/" + label + " is not open.");
        }
    }


    // provided interface for scope graphs, called by client

    @Override public void openRootEdges(S root, Iterable<L> labels) {
        assertInState(State.INIT);
        setState(State.ACTIVE);
        openEdges.putAll(root, labels);
        post(RootEdges.of(labels));
    }

    @Override public CompletableFuture<S> freshScope(String name, D datum, Iterable<L> labels) {
        assertInState(State.ACTIVE);
        final long messageId = post(FreshScope.of(name, datum, labels));
        final CompletableFuture<S> pendingScope = new CompletableFuture<>();
        pendingScopes.put(messageId, pendingScope);
        return pendingScope.whenComplete((scope, ex) -> {
            openEdges.putAll(scope, labels);
        });
    }

    @Override public final void addEdge(S source, L label, S target) {
        assertInState(State.ACTIVE);
        assertEdgeOpen(source, label);
        post(AddEdge.of(source, label, target));
    }

    @Override public final void closeEdge(S source, L label) {
        assertInState(State.ACTIVE);
        assertEdgeOpen(source, label);
        openEdges.remove(source, label);
        post(CloseEdge.of(source, label));
    }

    @Override public final CompletableFuture<java.util.Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF,
            DataWF<D> dataWF, LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        assertInState(State.ACTIVE);
        final long messageId = post(Query.of(scope, labelWF, dataWF, labelOrder, dataEquiv));
        final CompletableFuture<java.util.Set<IResolutionPath<S, L, D>>> pendingAnswer = new CompletableFuture<>();
        pendingQueries.put(messageId, pendingAnswer);
        return pendingAnswer;
    }

    // provided interface for process, called by client

    private final void done(R result) {
        logger.info("client {} done with {}", this, result);
        assertInState(State.ACTIVE);
        setState(State.DONE);
        post(Done.<S, L, D>builder().build());
        pendingResult.complete(result);
    }

    private final void failed(Throwable e) {
        logger.info("client {} failed with {}", this, e.getMessage());
        setState(State.FAILED);
        post(Failed.<S, L, D>builder().build());
        pendingResult.completeExceptionally(e);
    }


    @Override public String toString() {
        return "TypeChecker[" + resource + "]";
    }

}