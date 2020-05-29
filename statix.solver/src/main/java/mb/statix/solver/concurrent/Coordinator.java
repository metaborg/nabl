package mb.statix.solver.concurrent;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.HashTrieFunction;
import mb.nabl2.util.collections.HashTrieRelation2;
import mb.nabl2.util.collections.IFunction;
import mb.nabl2.util.collections.IRelation2;
import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSetMap;
import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.FastNameResolution;
import mb.statix.scopegraph.reference.IncompleteException;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.reference.ScopeGraph;
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
import mb.statix.solver.query.ResolutionDelayException;

public class Coordinator<S, L, D> implements CoordinatorMessage.Cases<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(Coordinator.class);

    private final Function2<String, String, S> newScope;
    private final S root;
    private final IScopeGraph.Transient<S, L, D> scopeGraph;
    private final MultiSetMap.Transient<S, EdgeOrData<L>> openEdges;

    public Coordinator(S root, Iterable<L> edgeLabels, Function2<String, String, S> fresh) {
        this.newScope = fresh;
        this.root = root;
        this.scopeGraph = ScopeGraph.Transient.<S, L, D>of(edgeLabels);
        this.openEdges = MultiSetMap.Transient.of();
    }

    private final CompletableFuture<Object> pendingResult = new CompletableFuture<>();

    private final BiMap<String, AbstractTypeChecker<S, L, D, ?>> clients = HashBiMap.create();
    private final MultiSetMap.Transient<String, String> scopeCounters = MultiSetMap.Transient.of();

    private final IFunction.Transient<AbstractTypeChecker<S, L, D, ?>, State> states = HashTrieFunction.Transient.of();

    private final BlockingQueue<CoordinatorMessage<S, L, D>> inbox = Queues.newLinkedBlockingDeque();
    private final MultiSet.Transient<AbstractTypeChecker<S, L, D, ?>> clocks = MultiSet.Transient.of(); // number of messages send to clients

    private final Collection<Query<S, L, D>> preInitQueries = Lists.newArrayList();
    private final IRelation2.Transient<Tuple2<S, EdgeOrData<L>>, Query<S, L, D>> delays =
            HashTrieRelation2.Transient.of();


    // client handling

    void register(AbstractTypeChecker<S, L, D, ?> client) {
        if(client.resource().isEmpty()) {
            throw new IllegalArgumentException("Resource must not be empty.");
        }
        clients.put(client.resource(), client);
    }

    private void setState(AbstractTypeChecker<S, L, D, ?> client, State state) throws InterruptedException {
        logger.info("believe client {} is in state {}", client, state);
        final State oldState = states.put(client, state);
        // start queries
        if(State.INIT.equals(oldState) && states.inverse().get(State.INIT).isEmpty()) {
            logger.info("release pre-init queries");
            for(Query<S, L, D> query : preInitQueries) {
                preInitQueries.remove(query);
                tryResolve(query);
            }
        }
    }

    private boolean hasRunningClients() {
        java.util.Set<AbstractTypeChecker<S, L, D, ?>> initClients = states.inverse().get(State.INIT);
        java.util.Set<AbstractTypeChecker<S, L, D, ?>> activeClients = states.inverse().get(State.ACTIVE);
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

    private void initClients() throws InterruptedException {
        for(AbstractTypeChecker<S, L, D, ?> client : clients.values()) {
            setState(client, State.INIT);
            post(client, Start.of(root));
        }
    }


    // name resolution

    private void tryResolve(Query<S, L, D> query) throws InterruptedException {
        if(!states.inverse().get(State.INIT).isEmpty()) {
            logger.info("delay pre-init query");
            preInitQueries.add(query);
            return;
        }
        logger.info("try resolve {}", query);
        try {
            // @formatter:off
            final INameResolution<S, L, D> nameResolution = FastNameResolution.<S, L, D>builder()
                        .withLabelWF(query.labelWF())
                        .withDataWF(query.dataWF())
                        .withLabelOrder(query.labelOrder())
                        .withDataEquiv(query.dataEquiv())
                        .withIsComplete((s, l) -> !openEdges.contains(s, l))
                        .build(scopeGraph);
            // @formatter:on
            final Env<S, L, D> paths = nameResolution.resolve(query.scope());
            post(query.client(), QueryAnswer.of(query.id(), paths));
        } catch(IncompleteException e) {
            final S scope = e.scope();
            final EdgeOrData<L> label = e.<L>label();
            logger.info("delay query on edge {}/{}", scope, label);
            delays.put(Tuple2.of(scope, label), query);
        } catch(ResolutionDelayException e) {
            logger.warn("treating unhandleable exception as deadlock.", e);
            post(query.client(), DeadLock.of(query.id()));
        } catch(ResolutionException e) {
            logger.warn("treating unhandleable exception as deadlock.", e);
            post(query.client(), DeadLock.of(query.id()));
        }
    }

    private void releaseDelays(S source, EdgeOrData<L> label) throws InterruptedException {
        logger.info("release queries for edge {}/{}", source, label);
        final Set<Query<S, L, D>> queries = delays.removeKey(Tuple2.of(source, label));
        for(Query<S, L, D> query : queries) {
            if(!delays.containsValue(query)) {
                logger.info("released {}", query);
                tryResolve(query);
            }
        }
    }


    // message handling

    void receive(CoordinatorMessage<S, L, D> message) {
        logger.info("{} sent {}", message.client(), message);
        inbox.add(message);
    }

    private void post(AbstractTypeChecker<S, L, D, ?> client, ClientMessage<S, L, D> message) {
        logger.info("post {} to client {}", message, client);
        clocks.add(client);
        client.receive(message);
    }

    @Override public void on(RootEdges<S, L, D> message) throws InterruptedException {
        logger.info("client {} open root edges {}", message.client(), message.labels());
        openEdges.putAll(root, message.labels().stream().map(EdgeOrData::edge).collect(Collectors.toSet()));
        setState(message.client(), State.ACTIVE);
    }

    @Override public void on(FreshScope<S, L, D> message) throws InterruptedException {
        final String resource = clients.inverse().get(message.client());
        final String name = message.name().replace("-", "_");
        final int n = scopeCounters.put(resource, name);
        final S scope = newScope.apply(resource, name + "-" + n);
        logger.info("client {} fresh scope {} -> {} with open edges {}", message.client(), scope, message.datum(),
                message.labels());
        scopeGraph.setDatum(scope, message.datum());
        openEdges.putAll(root, message.labels().stream().map(EdgeOrData::edge).collect(Collectors.toSet()));
        post(message.client(), ScopeAnswer.of(message.id(), scope));
    }

    @Override public void on(AddEdge<S, L, D> message) throws InterruptedException {
        logger.info("client {} add edge {} -{}-> {}", message.client(), message.source(), message.label(),
                message.target());
        scopeGraph.addEdge(message.source(), message.label(), message.target());
    }

    @Override public void on(CloseEdge<S, L, D> message) throws InterruptedException {
        logger.info("client {} close edge {}/{}", message.client(), message.source(), message.label());
        final int n = openEdges.remove(message.source(), EdgeOrData.edge(message.label()));
        if(n == 0) {
            logger.info("edge {}/{} closed", message.source(), message.label());
            releaseDelays(message.source(), EdgeOrData.edge(message.label()));
        }
    }

    @Override public void on(Query<S, L, D> message) throws InterruptedException {
        tryResolve(message);
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

    @Override public void on(Failed<S, L, D> message) throws InterruptedException {
        setState(message.client(), State.FAILED);
    }


    @Override public String toString() {
        return "Coordinator";
    }

}