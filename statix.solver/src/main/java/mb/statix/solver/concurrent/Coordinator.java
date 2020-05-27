package mb.statix.solver.concurrent;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.metaborg.util.functions.Function2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Queues;

import mb.nabl2.util.collections.HashTrieFunction;
import mb.nabl2.util.collections.IFunction;
import mb.nabl2.util.collections.MultiSet;
import mb.statix.solver.concurrent.messages.AddEdge;
import mb.statix.solver.concurrent.messages.ClientMessage;
import mb.statix.solver.concurrent.messages.CloseEdge;
import mb.statix.solver.concurrent.messages.CoordinatorMessage;
import mb.statix.solver.concurrent.messages.Done;
import mb.statix.solver.concurrent.messages.FreshScope;
import mb.statix.solver.concurrent.messages.Query;
import mb.statix.solver.concurrent.messages.ScopeAnswer;
import mb.statix.solver.concurrent.messages.Start;
import mb.statix.solver.concurrent.messages.StartAnswer;
import mb.statix.solver.concurrent.messages.Suspend;

public class Coordinator<S, L, D> implements CoordinatorMessage.Cases<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(Coordinator.class);

    private final Function2<String, Integer, S> newScope;
    private final S root;

    public Coordinator(S root, Function2<String, Integer, S> fresh) {
        this.newScope = fresh;
        this.root = root;
    }

    private final CompletableFuture<Object> pendingResult = new CompletableFuture<>();

    private final BiMap<String, AbstractTypeChecker<S, L, D, ?>> clients = HashBiMap.create();
    private final MultiSet.Transient<String> scopeCounters = MultiSet.Transient.of();

    private final IFunction.Transient<AbstractTypeChecker<S, L, D, ?>, State> states = HashTrieFunction.Transient.of();

    private final BlockingQueue<CoordinatorMessage<S, L, D>> inbox = Queues.newLinkedBlockingDeque();
    private final MultiSet.Transient<AbstractTypeChecker<S, L, D, ?>> clocks = MultiSet.Transient.of(); // number of messages send to clients

    void register(AbstractTypeChecker<S, L, D, ?> client) {
        if(client.resource().isEmpty()) {
            throw new IllegalArgumentException("Resource must not be empty.");
        }
        clients.put(client.resource(), client);
    }

    void receive(CoordinatorMessage<S, L, D> message) {
        logger.info("{} sent {}", message.client(), message);
        inbox.add(message);
    }

    private void post(AbstractTypeChecker<S, L, D, ?> client, ClientMessage<S, L, D> message) {
        logger.info("post {} to client {}", message, client);
        clocks.add(client);
        client.receive(message);
    }

    private void setState(AbstractTypeChecker<S, L, D, ?> client, State state) {
        logger.info("believe client {} is in state {}", client, state);
        states.put(client, state);
    }

    private boolean hasRunningClients() {
        Set<AbstractTypeChecker<S, L, D, ?>> initClients = states.inverse().get(State.INIT);
        Set<AbstractTypeChecker<S, L, D, ?>> activeClients = states.inverse().get(State.ACTIVE);
        return !initClients.isEmpty() || !activeClients.isEmpty();
    }

    public CompletableFuture<Object> run(ExecutorService executorService) {
        executorService.submit(() -> {
            try {
                run();
            } catch(Throwable e) {
                pendingResult.completeExceptionally(e);
            }
        });
        return pendingResult;
    }

    private void run() throws InterruptedException {
        initClients();
        while(hasRunningClients()) {
            final CoordinatorMessage<S, L, D> message = inbox.take();
            logger.info("processing {}", message);
            message.match(this);
        }
        logger.info("coordinator finished.");
        pendingResult.complete(new Object());
    }

    private void initClients() {
        for(AbstractTypeChecker<S, L, D, ?> client : clients.values()) {
            setState(client, State.INIT);
            post(client, Start.of(root));
        }
    }

    @Override public void on(StartAnswer<S, L, D> message) throws InterruptedException {
        // TODO add open edges
        setState(message.client(), State.ACTIVE);
    }

    @Override public void on(FreshScope<S, L, D> message) throws InterruptedException {
        final String resource = clients.inverse().get(message.client());
        final S scope = newScope.apply(resource, scopeCounters.add(resource));
        post(message.client(), ScopeAnswer.of(message.id(), scope));
    }

    @Override public void on(AddEdge<S, L, D> message) throws InterruptedException {
        // TODO add edge
    }

    @Override public void on(CloseEdge<S, L, D> message) throws InterruptedException {
        // TODO close edge
    }

    @Override public void on(Query<S, L, D> message) throws InterruptedException {
        // TODO try query
    }

    @Override public void on(Suspend<S, L, D> message) throws InterruptedException {
        if(message.clock() < clocks.count(message.client())) {
            // we have sent messages since
            return;
        }
        logger.info("client {} suspended", message.client());
        // deadlock detection
    }

    @Override public void on(Done<S, L, D> message) throws InterruptedException {
        setState(message.client(), State.DONE);
    }

    @Override public String toString() {
        return "Coordinator";
    }

}