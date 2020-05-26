package mb.statix.solver.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.metaborg.util.functions.Function2;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Queues;

import mb.nabl2.util.collections.MultiSet;
import mb.statix.solver.concurrent.messages.AddEdge;
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

    private final Function2<String, Integer, S> newScope;
    private final S root;

    public Coordinator(Function2<String, Integer, S> fresh) {
        this.newScope = fresh;
        this.root = fresh.apply("", 0);
    }

    private final CompletableFuture<Object> pendingResult = new CompletableFuture<>();

    private final BiMap<String, AbstractTypeChecker<S, L, D>> clients = HashBiMap.create();
    private final MultiSet.Transient<String> scopeCounters = MultiSet.Transient.of();

    private final BlockingQueue<CoordinatorMessage<S, L, D>> inbox = Queues.newLinkedBlockingDeque();

    void register(String resource, AbstractTypeChecker<S, L, D> client) {
        if(resource.isEmpty()) {
            throw new IllegalArgumentException("Resource must not be empty.");
        }
        clients.put(resource, client);
    }

    void post(CoordinatorMessage<S, L, D> message) {
        inbox.add(message);
    }

    public CompletableFuture<Object> run(ExecutorService executorService) {
        executorService.submit(() -> {
            try {
                run();
            } catch(InterruptedException e) {
                pendingResult.completeExceptionally(e);
            }
        });
        return pendingResult;
    }

    private void run() throws InterruptedException {
        initClients();
        while(true) {
            final CoordinatorMessage<S, L, D> message = inbox.take();
            message.match(this);
        }
    }

    private void initClients() {
        for(AbstractTypeChecker<S, L, D> client : clients.values()) {
            client.post(Start.of(root));
        }
    }

    @Override public void on(StartAnswer<S, L, D> message) throws InterruptedException {
        // TODO add open edges
    }

    @Override public void on(FreshScope<S, L, D> message) throws InterruptedException {
        final String resource = clients.inverse().get(message.client());
        final S scope = newScope.apply(resource, scopeCounters.add(resource));
        message.client().post(ScopeAnswer.of(message.id(), scope));
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
        // ignore if suspend is too old
        // change state
    }

    @Override public void on(Done<S, L, D> message) throws InterruptedException {
        // change state
    }

}