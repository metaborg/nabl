package mb.statix.concurrent._attic;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.RateLimitedCancel;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.HashTrieFunction;
import mb.nabl2.util.collections.IFunction;
import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSetMap;
import mb.statix.concurrent._attic.messages.AddEdge;
import mb.statix.concurrent._attic.messages.ClientMessage;
import mb.statix.concurrent._attic.messages.CloseEdge;
import mb.statix.concurrent._attic.messages.CoordinatorMessage;
import mb.statix.concurrent._attic.messages.Done;
import mb.statix.concurrent._attic.messages.Failed;
import mb.statix.concurrent._attic.messages.FreshScope;
import mb.statix.concurrent._attic.messages.Query;
import mb.statix.concurrent._attic.messages.QueryAnswer;
import mb.statix.concurrent._attic.messages.QueryFailed;
import mb.statix.concurrent._attic.messages.RootEdges;
import mb.statix.concurrent._attic.messages.ScopeAnswer;
import mb.statix.concurrent._attic.messages.SetDatum;
import mb.statix.concurrent._attic.messages.Start;
import mb.statix.concurrent._attic.messages.Suspend;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.reference.Access;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.FastNameResolution;
import mb.statix.scopegraph.reference.IncompleteException;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.reference.ScopeGraph;

public class Coordinator<S, L, D> implements CoordinatorMessage.Cases<S, L, D> {

    private static final ILogger logger = LoggerUtils.logger(Coordinator.class);

    private static final String ROOT_RESOURCE = "<>";


    private final ScopeImpl<S> scopeImpl;
    private final S rootScope;
    private final Set.Immutable<L> edgeLabels;
    private final SetMultimap.Transient<String, S> unitScopes;
    private final Map.Transient<String, IScopeGraph.Transient<S, L, D>> scopeGraphs;
    private final Map.Transient<String, MultiSetMap.Transient<S, EdgeOrData<L>>> openEdges;

    final ICancel cancel;


    private final CompletableFuture<CoordinatorResult<S, L, D>> pendingResult = new CompletableFuture<>();

    private final BiMap<String, AbstractTypeChecker<S, L, D, ?>> clients = HashBiMap.create();
    private final MultiSetMap.Transient<String, String> scopeCounters = MultiSetMap.Transient.of();

    private final IFunction.Transient<String, State> states = HashTrieFunction.Transient.of();
    private final Set.Transient<String> waiting = Set.Transient.of();

    private final BlockingQueue<CoordinatorMessage<S, L, D>> inbox = Queues.newLinkedBlockingDeque();
    private final MultiSet.Transient<AbstractTypeChecker<S, L, D, ?>> clocks = MultiSet.Transient.of(); // number of messages send to clients

    private final Collection<Query<S, L, D>> preInitQueries = Lists.newArrayList();
    private final DelayGraph<S, L, D> delays;


    public Coordinator(Iterable<L> edgeLabels, ScopeImpl<S> scopeImpl, ICancel cancel) {
        this.scopeImpl = scopeImpl;
        this.rootScope = scopeImpl.make(ROOT_RESOURCE, "0");
        this.edgeLabels = CapsuleUtil.toSet(edgeLabels);
        this.unitScopes = SetMultimap.Transient.of();
        this.scopeGraphs = Map.Transient.of();
        this.openEdges = Map.Transient.of();
        this.cancel = new RateLimitedCancel(cancel, 42);
        this.delays = new DelayGraph<>(scopeImpl);
        registerRoot();
    }

    // act as aggregate of all unit scope graphs
    private final IScopeGraph<S, L, D> scopeGraph = new IScopeGraph<S, L, D>() {

        @Override public Set.Immutable<L> getEdgeLabels() {
            return edgeLabels;
        }

        @Override public java.util.Map<? extends Entry<S, L>, ? extends Iterable<S>> getEdges() {
            throw new UnsupportedOperationException();
        }

        @Override public Iterable<S> getEdges(S scope, L label) {
            final String owner = scopeImpl.resource(scope);
            return scopeGraphs.get(owner).getEdges(scope, label);
        }

        @Override public java.util.Map<S, D> getData() {
            throw new UnsupportedOperationException();
        }

        @Override public Optional<D> getData(S scope) {
            final String owner = scopeImpl.resource(scope);
            return scopeGraphs.get(owner).getData(scope);
        }
    };

    ////////////////////////////////////////////////////////////////////////////
    // client handling
    ////////////////////////////////////////////////////////////////////////////

    void registerRoot() {
        scopeGraphs.__put(ROOT_RESOURCE, ScopeGraph.Transient.<S, L, D>of(edgeLabels));
        openEdges.__put(ROOT_RESOURCE, MultiSetMap.Transient.of());
        delays.addUnits(Iterables2.singleton(ROOT_RESOURCE));
    }

    void register(AbstractTypeChecker<S, L, D, ?> client) {
        final String resource = client.resource();
        if(resource.isEmpty()) {
            throw new IllegalArgumentException("Resource must not be empty.");
        }
        clients.put(resource, client);
        scopeGraphs.__put(resource, ScopeGraph.Transient.<S, L, D>of(edgeLabels));
        openEdges.__put(resource, MultiSetMap.Transient.of());
        delays.addUnits(Iterables2.singleton(resource));
    }

    private void setState(AbstractTypeChecker<S, L, D, ?> client, State state) throws InterruptedException {
        final String resource = client.resource();
        logger.info("believe client {} is in state {}", client, state);
        final State oldState = states.put(resource, state);
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
        java.util.Set<String> initClients = states.inverse().get(State.INIT);
        java.util.Set<String> activeClients = states.inverse().get(State.ACTIVE);
        return !initClients.isEmpty() || !activeClients.isEmpty();
    }

    public IFuture<CoordinatorResult<S, L, D>> run(ExecutorService executorService) {
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

        final Map.Transient<String, IScopeGraph.Immutable<S, L, D>> finalScopeGraphs = Map.Transient.of();
        scopeGraphs.forEach((r, sg) -> finalScopeGraphs.__put(r, sg.freeze()));
        pendingResult.completeValue(CoordinatorResult.of(finalScopeGraphs.freeze()));
    }

    private void initClients() throws InterruptedException {
        for(AbstractTypeChecker<S, L, D, ?> client : clients.values()) {
            setState(client, State.INIT);
            post(client, Start.of(rootScope));
        }
    }


    ////////////////////////////////////////////////////////////////////////////
    // name resolution
    ////////////////////////////////////////////////////////////////////////////

    private void tryResolve(Query<S, L, D> query) throws InterruptedException {
        final AbstractTypeChecker<S, L, D, ?> client = query.client();
        final String resource = client.resource();

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
                        .withIsComplete((s, l) -> {
                            final String owner = scopeImpl.resource(s);
                            final EdgeOrData<L> edge = fixEdgeOrData(resource, s, l);
                            return !openEdges.get(owner).contains(s, edge);
                        })
                        .build(scopeGraph);
            // @formatter:on
            final Env<S, L, D> paths = nameResolution.resolve(query.scope(), cancel);
            post(client, QueryAnswer.of(query.id(), paths));
        } catch(IncompleteException e) {
            final S scope = e.scope();
            final EdgeOrData<L> label = fixEdgeOrData(resource, scope, e.<L>label());
            logger.info("delay query of {} on edge {}/{}", client, scope, label);
            delays.addDelayOnEdge(query, scope, label);
        } catch(ResolutionException e) {
            logger.warn("forwarding resolution exception.", e);
            post(client, QueryFailed.of(query.id(), e));
        }
    }

    private EdgeOrData<L> fixEdgeOrData(String resource, S scope, EdgeOrData<L> edgeOrData) {
        // @formatter:off
        return edgeOrData.match(
            acc -> {
                final String owner = scopeImpl.resource(scope);
                return owner.equals(resource) ? edgeOrData : EdgeOrData.data(Access.EXTERNAL);
            },
            lbl -> {
                return edgeOrData;
            }
        );
        // @formatter:on
    }

    private void closeEdge(S source, EdgeOrData<L> label) throws InterruptedException {
        logger.info("release queries for edge {}/{}", source, label);
        for(Query<S, L, D> query : delays.removeEdge(source, label)) {
            logger.info("released {}", query);
            tryResolve(query);
        }
    }


    ////////////////////////////////////////////////////////////////////////////
    // message handling
    ////////////////////////////////////////////////////////////////////////////

    void receive(CoordinatorMessage<S, L, D> message) {
        logger.info("{} sent {}", message.client(), message);
        inbox.add(message);
    }

    private void post(AbstractTypeChecker<S, L, D, ?> client, ClientMessage<S, L, D> message) {
        logger.info("post {} to client {}", message, client);
        waiting.__remove(client.resource());
        clocks.add(client);
        client.receive(message);
    }

    @Override public void on(RootEdges<S, L, D> message) throws InterruptedException {
        final AbstractTypeChecker<S, L, D, ?> client = message.client();
        final String resource = client.resource();

        logger.info("client {} open root edges {}", client, message.labels());
        final java.util.Set<EdgeOrData<L>> edges =
                message.labels().stream().map(EdgeOrData::edge).collect(Collectors.toSet());
        final String owner = ROOT_RESOURCE;
        openEdges.get(owner).putAll(rootScope, edges);
        if(!owner.equals(resource)) {
            openEdges.get(resource).putAll(rootScope, edges);
        }
        setState(client, State.ACTIVE);
    }

    @Override public void on(FreshScope<S, L, D> message) throws InterruptedException {
        final String resource = message.client().resource();
        final String name = message.name().replace("-", "_");
        final int n = scopeCounters.put(resource, name);
        final S scope = scopeImpl.make(resource, name + "-" + n);
        final ImmutableSet<L> labels = message.labels();
        final ImmutableSet<Access> accesses = message.accesses();

        logger.info("client {} fresh scope {} -> {} with open edges {}, data {}", message.client(), scope, labels,
                accesses);
        unitScopes.__insert(resource, scope);
        openEdges.get(resource).putAll(scope, labels.stream().map(EdgeOrData::edge).collect(Collectors.toSet()));
        openEdges.get(resource).putAll(scope, accesses.stream().map(EdgeOrData::<L>data).collect(Collectors.toSet()));
        post(message.client(), ScopeAnswer.of(message.id(), scope));
    }

    @Override public void on(SetDatum<S, L, D> message) throws InterruptedException {
        final AbstractTypeChecker<S, L, D, ?> client = message.client();
        final String resource = client.resource();
        final S scope = message.scope();
        final D datum = message.datum();
        final Access access = message.access();

        logger.info("client {} set datum {} : {} {}", client, scope, datum, access);
        // assert owner = resource 
        scopeGraphs.get(resource).setDatum(scope, datum);
        final int n = openEdges.get(resource).remove(scope, EdgeOrData.data(access));
        if(n == 0) {
            logger.info("datum {}/{} closed", scope, access);
            closeEdge(scope, EdgeOrData.data(access));
        }
    }

    @Override public void on(AddEdge<S, L, D> message) throws InterruptedException {
        final AbstractTypeChecker<S, L, D, ?> client = message.client();
        final String resource = client.resource();
        final S source = message.source();
        final L label = message.label();
        final S target = message.target();

        logger.info("client {} add edge {} -{}-> {}", client, source, label, target);
        final String owner = scopeImpl.resource(source);
        scopeGraphs.get(owner).addEdge(source, label, target);
        if(!owner.equals(resource)) {
            scopeGraphs.get(resource).addEdge(source, label, target);
        }
    }

    @Override public void on(CloseEdge<S, L, D> message) throws InterruptedException {
        final AbstractTypeChecker<S, L, D, ?> client = message.client();
        final String resource = client.resource();
        final S source = message.source();
        final L label = message.label();

        logger.info("client {} close edge {}/{}", client, source, label);
        final String owner = scopeImpl.resource(source);
        final int n = openEdges.get(owner).remove(source, EdgeOrData.edge(label));
        if(!owner.equals(resource)) {
            openEdges.get(resource).remove(source, EdgeOrData.edge(label));
        }
        if(n == 0) {
            logger.info("edge {}/{} closed", source, label);
            closeEdge(source, EdgeOrData.edge(label));
        }
    }

    @Override public void on(Query<S, L, D> message) throws InterruptedException {
        tryResolve(message);
    }

    @Override public void on(Suspend<S, L, D> message) throws InterruptedException {
        final AbstractTypeChecker<S, L, D, ?> client = message.client();
        final String resource = client.resource();

        if(message.clock() < clocks.count(client)) {
            // we have sent messages since
            logger.info("client {} suspended: ignored", client);
            return;
        }

        final java.util.Set<Tuple2<S, EdgeOrData<L>>> unitEdges = openEdges.get(resource).toMap().entrySet().stream()
                .flatMap(e -> e.getValue().elementSet().stream().map(l -> Tuple2.of(e.getKey(), l)))
                .collect(Collectors.toSet());

        final int unitDelays = delays.getDelaysOfUnit(resource).size();
        logger.info("client {} suspended: pending {} queries, {} open edges", client, unitDelays, unitEdges.size());
        logger.info("client {} open edges: {}", client, unitEdges);

        waiting.__insert(resource);

        detectDeadLock(resource);
    }

    @Override public void on(Done<S, L, D> message) throws InterruptedException {
        setState(message.client(), State.DONE);

        // FIXME detectDeadlock()?
        // A unit can only be done when all its edges have been closed before.
        // There should be no more queries delayed on this unit, so no
        // deadlock should be able to occur because of this.
    }

    @Override public void on(Failed<S, L, D> message) throws InterruptedException {
        final AbstractTypeChecker<S, L, D, ?> client = message.client();
        final String resource = client.resource();

        setState(client, State.FAILED);

        // remove delayed queries from this unit
        delays.removeUnit(resource);

        // remove delayed queries on this unit
        final MultiSetMap.Transient<S, EdgeOrData<L>> edges = openEdges.__remove(resource);
        for(Entry<S, MultiSet.Immutable<EdgeOrData<L>>> entry : edges.toMap().entrySet()) {
            for(EdgeOrData<L> edge : entry.getValue().elementSet()) {
                for(Query<S, L, D> query : delays.removeEdge(entry.getKey(), edge)) {
                    tryResolve(query);
                }
            }
        }

        // FIXME detectDeadlock?
        // The failed unit closes all its edges, possible triggering queries
        // delayed on the unit. No deadlock can occur?
    }

    ////////////////////////////////////////////////////////////////////////////
    // deadlock
    ////////////////////////////////////////////////////////////////////////////

    private void detectDeadLock(String resource) {
        logger.info("Detect deadlock for {}", resource);
        final java.util.Set<String> scc = delays.getComponent(resource);
        if(delays.inPeninsula(resource)
                && scc.stream().allMatch(c -> states.get(c).get().equals(State.ACTIVE) && waiting.contains(c))) {
            logger.warn("Detected deadlock for SCC {}", scc);
            for(String c : scc) {
                for(Query<S, L, D> query : delays.removeUnit(c)) {
                    post(query.client(), QueryFailed.of(query.id(), new DeadLockedException("deadlock")));
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Object
    ////////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return "Coordinator";
    }

    ////////////////////////////////////////////////////////////////////////////
    // result classes
    ////////////////////////////////////////////////////////////////////////////

    @Value.Immutable
    public static abstract class ACoordinatorResult<S, L, D> {

        @Value.Parameter public abstract Map.Immutable<String, IScopeGraph.Immutable<S, L, D>> scopeGraph();

    }

}