package mb.statix.scopegraph.bottomup;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.unit.Unit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Queues;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.nabl2.util.collections.HashTrieRelation2;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation2;
import mb.nabl2.util.collections.IRelation3;
import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.graph.alg.incscc.IncSCCAlg;
import mb.nabl2.util.graph.graphimpl.Graph;
import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IStep;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.IncompleteException;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.reference.StuckException;
import mb.statix.scopegraph.terms.path.Paths;
import mb.statix.solver.CriticalEdge;

public class BUNameResolution<S, L, D> implements INameResolution<S, L, D> {

    @SuppressWarnings("unused") private static final ILogger logger = LoggerUtils.logger(BUNameResolution.class);

    private final IScopeGraph<S, L, D> scopeGraph;
    private final Iterable<L> edgeLabels;
    private final Predicate2<S, EdgeOrData<L>> isClosed;

    private final Cache<S, Collection<D>> visibles = CacheBuilder.newBuilder().build();
    private final Cache<S, Collection<D>> reachables = CacheBuilder.newBuilder().build();

    public BUNameResolution(IScopeGraph<S, L, D> scopeGraph, Predicate2<S, EdgeOrData<L>> isClosed) {
        this.scopeGraph = scopeGraph;
        this.edgeLabels = Collections.EMPTY_LIST; // FXIME
        this.isClosed = isClosed;
    }

    //    public static <S, L, D> BUNameResolution<S, L, D> of(IScopeGraph<S, L, D> scopeGraph, Predicate2<S, L> isClosed,
    //            BUCache<S, L, D> cache) {
    //        final BUNameResolution<S, L, D> nr = new BUNameResolution<>(scopeGraph, isClosed);
    //        if(cache instanceof BUCache) {
    //            final BUCache<S, L, D> _cache = (BUCache<S, L, D>) cache;
    //            nr.envKeys.__putAll(_cache.envKeys);
    //            nr.pathKeys.__putAll(_cache.pathKeys);
    //            _cache.envs.forEach((env, ps) -> nr.envs.__put(env, new BUEnv<>(env.kind.order(), ps)));
    //            nr.envs.keySet().forEach(e -> nr.depGraph.insertNode(e));
    //            nr.completed.__insertAll(_cache.completed);
    //            nr.backedges.putAll(_cache.backedges);
    //            nr.backedges.stream().forEach(be -> nr.depGraph.insertEdge(be._3(), be._1()));
    //            nr.openEdges.putAll(_cache.openEdges);
    //        }
    //        return nr;
    //    }

    public static <S, L, D> BUNameResolution<S, L, D> of(IScopeGraph<S, L, D> scopeGraph,
            Predicate2<S, EdgeOrData<L>> isClosed) {
        return new BUNameResolution<>(scopeGraph, isClosed);
    }

    ///////////////////////////////////////////////////////////////////////////
    // INameResolution
    ///////////////////////////////////////////////////////////////////////////

    @Override public Env<S, L, D> resolve(S scope, ICancel cancel) throws ResolutionException, InterruptedException {
        throw new UnsupportedOperationException();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation
    ///////////////////////////////////////////////////////////////////////////

    private final Map.Transient<Tuple3<BUEnvKind<L, D>, S, IRegExp<L>>, BUEnvKey<S, L, D>> envKeys = Map.Transient.of();
    private final Map.Transient<Tuple2<SpacedName, EdgeOrData<L>>, BUPathKey<L>> pathKeys = Map.Transient.of();
    private final Map.Transient<BUEnvKey<S, L, D>, BUEnv<S, L, D, IResolutionPath<S, L, D>>> envs = Map.Transient.of();
    private final Set.Transient<BUEnvKey<S, L, D>> completed = Set.Transient.of();
    private final IRelation2.Transient<BUEnvKey<S, L, D>, CriticalEdge> openEdges = HashTrieRelation2.Transient.of();
    private final IRelation3.Transient<BUEnvKey<S, L, D>, IStep<S, L>, BUEnvKey<S, L, D>> backedges =
            HashTrieRelation3.Transient.of();

    private final Deque<InterruptibleRunnable> worklist = Queues.newArrayDeque();
    private final MultiSet.Transient<BUEnvKey<S, L, D>> pendingChanges = MultiSet.Transient.of();
    private final Graph<BUEnvKey<S, L, D>> depGraph = new Graph<>();
    private final IncSCCAlg<BUEnvKey<S, L, D>> sccGraph = new IncSCCAlg<>(depGraph);

    private Collection<IResolutionPath<S, L, D>> resolve(S scope, BUEnvKind<L, D> kind, SpacedName ref, ICancel cancel)
            throws InterruptedException, IncompleteException, StuckException {
        final BUEnvKey<S, L, D> key = envKey(kind, scope, kind.wf());
        final BUEnv<S, L, D, IResolutionPath<S, L, D>> env = getOrCompute(key, cancel);
        return env.pathSet().paths(ref).stream().collect(CapsuleCollectors.toSet());
    }

    /**
     * Get or compute the complete environment for the given key. If the environment is incomplete, throws a critical
     * edge exception.
     */
    private BUEnv<S, L, D, IResolutionPath<S, L, D>> getOrCompute(BUEnvKey<S, L, D> env, ICancel cancel)
            throws InterruptedException, IncompleteException, StuckException {
        final BUEnv<S, L, D, IResolutionPath<S, L, D>> _env = init(env);
        work(cancel);
        throwIfIncomplete(env);
        return _env;
    }

    @SuppressWarnings("unchecked") public void update(Iterable<CriticalEdge> criticalEdges, ICancel cancel,
            IProgress progress) throws InterruptedException {
        for(CriticalEdge ce : criticalEdges) {
            if(!isClosed((S) ce.scope(), (EdgeOrData<L>) ce.edgeOrData())) {
                continue;
            }
            for(BUEnvKey<S, L, D> env : openEdges.removeValue(ce)) {
                final BUPathSet.Transient<S, L, D, IResolutionPath<S, L, D>> declPaths = BUPathSet.Transient.of();
                ce.edgeOrData().match(() -> {
                    initData(env, declPaths);
                    return Unit.unit;
                }, l -> {
                    initEdge(env, (L) l, env.wf.match((L) l));
                    return Unit.unit;
                });
                queueChanges(env, new BUChanges<>(declPaths.freeze(), BUPathSet.Immutable.of()));
            }
        }
        work(cancel);
    }

    private void work(ICancel cancel) throws InterruptedException {
        while(!worklist.isEmpty()) {
            cancel.throwIfCancelled();
            worklist.pop().run(cancel);
        }
    }

    private BUEnv<S, L, D, IResolutionPath<S, L, D>> init(BUEnvKey<S, L, D> env) {
        BUEnv<S, L, D, IResolutionPath<S, L, D>> _env = envs.get(env);
        if(_env != null) {
            return _env;
        }
        logger.trace("init {}", env);
        envs.__put(env, _env = new BUEnv<>(env.kind.order()));
        depGraph.insertNode(env);
        final BUPathSet.Transient<S, L, D, IResolutionPath<S, L, D>> declPaths = BUPathSet.Transient.of();
        if(env.wf.isAccepting()) {
            if(isClosed(env.scope, EdgeOrData.data())) {
                initData(env, declPaths);
            } else {
                openEdges.put(env, CriticalEdge.of((ITerm) env.scope, EdgeOrData.data()));
            }
        }
        for(L l : edgeLabels) {
            IRegExpMatcher<L> nextWf = env.wf.match(l);
            if(nextWf.isEmpty()) {
                continue;
            }
            if(isClosed(env.scope, EdgeOrData.edge(l))) {
                initEdge(env, l, nextWf);
            } else {
                openEdges.put(env, CriticalEdge.of((ITerm) env.scope, EdgeOrData.edge((ITerm) l)));
            }
        }
        logger.trace("queued init changes {} added {}", env, declPaths.names());
        queueChanges(env, new BUChanges<>(declPaths.freeze(), BUPathSet.Immutable.of()));
        queueCheckComplete(env);
        return _env;
    }

    private void initData(BUEnvKey<S, L, D> env, BUPathSet.Transient<S, L, D, IResolutionPath<S, L, D>> declPaths) {
        logger.trace("init data {}", env);
        final D datum;
        if((datum = scopeGraph.getData(env.scope).orElse(null)) != null) {
            final BUPathKey<L> key = pathKey(env.kind.index(datum), EdgeOrData.data());
            declPaths.add(key, Paths.resolve(Paths.empty(env.scope), datum));
        }
        logger.trace("inited data {}", env);
    }

    private void initEdge(BUEnvKey<S, L, D> env, L l, IRegExpMatcher<L> nextWf) {
        logger.trace("init edges {} label {} next wf {}", env, l, nextWf);
        for(S scope : scopeGraph.getEdges(env.scope, l)) {
            final BUEnvKey<S, L, D> srcEnv = envKey(env.kind, scope, nextWf);
            init(srcEnv);
            addBackEdge(srcEnv, Paths.edge(env.scope, l, srcEnv.scope), env);
        }
        logger.trace("inited edge {} next wf {}", env, nextWf);
    }

    private void queueChanges(BUEnvKey<S, L, D> env, BUChanges<S, L, D, IResolutionPath<S, L, D>> changes) {
        if(completed.contains(env)) {
            throw new IllegalStateException();
        }
        if(changes.isEmpty()) {
            return;
        }
        pendingChanges.add(env);
        worklist.add((cancel) -> processChanges(env, changes));
    }

    private void processChanges(BUEnvKey<S, L, D> env, BUChanges<S, L, D, IResolutionPath<S, L, D>> changes)
            throws InterruptedException {
        if(completed.contains(env)) {
            throw new IllegalStateException();
        }
        pendingChanges.remove(env);
        logger.trace("process changes {} added {} removed {}", env, changes.addedPaths(), changes.removedPaths());
        final BUEnv<S, L, D, IResolutionPath<S, L, D>> _env = envs.get(env);
        _env.apply(changes);
        if(_env.hasChanges() && !pendingChanges.contains(env)) {
            final BUChanges<S, L, D, IResolutionPath<S, L, D>> newChanges = _env.commitChanges();
            for(Entry<IStep<S, L>, BUEnvKey<S, L, D>> entry : backedges.get(env)) {
                final BUEnvKey<S, L, D> dstEnv = entry.getValue();
                final IStep<S, L> step = entry.getKey();
                final BUChanges<S, L, D, IResolutionPath<S, L, D>> envChanges = newChanges.flatMap((k, ps) -> {
                    final List<IResolutionPath<S, L, D>> newPs = ps.stream()
                            .flatMap(p -> (env.kind.arePathsRelevant() ? ofOpt(Paths.append(step, p)) : Stream.of(p)))
                            .collect(Collectors.toList());
                    return Tuple2.of(pathKey(k.name(), EdgeOrData.edge(step.getLabel())), newPs);
                });
                logger.trace("queued fwd changes {} to {} added {} removed {}", env, dstEnv,
                        envChanges.addedPaths().paths(), envChanges.removedPaths().paths());
                queueChanges(dstEnv, envChanges);
            }
        }
        checkComplete(env);
    }

    private void queueCheckComplete(BUEnvKey<S, L, D> env) {
        if(completed.contains(env)) {
            return;
        }
        worklist.add((cancel) -> checkComplete(env));
    }

    private void checkComplete(BUEnvKey<S, L, D> env) throws InterruptedException {
        if(completed.contains(env)) {
            return;
        }
        if(isComplete(env)) {
            completed.__insert(env);
            final BUEnv<S, L, D, IResolutionPath<S, L, D>> _env = envs.get(env);
            logger.trace("completed {} {}", env, _env.pathSet());
            for(Entry<IStep<S, L>, BUEnvKey<S, L, D>> entry : backedges.remove(env).entrySet()) {
                final BUEnvKey<S, L, D> dstEnv = entry.getValue();
                depGraph.deleteEdgeThatExists(dstEnv, env);
                queueCheckComplete(dstEnv);
            }
        }
    }

    private void addBackEdge(BUEnvKey<S, L, D> srcEnv, IStep<S, L> step, BUEnvKey<S, L, D> dstEnv) {
        logger.trace("add back edge {} {} {}", dstEnv, step, srcEnv);
        if(!completed.contains(srcEnv)) {
            if(backedges.put(srcEnv, step, dstEnv)) {
                depGraph.insertEdge(dstEnv, srcEnv);
                logger.trace(" * new edge");
            } else {
                logger.trace(" * duplicate edge");
                return;
            }
        } else {
            logger.trace(" * complete edge");
        }

        final BUEnv<S, L, D, IResolutionPath<S, L, D>> _env = envs.get(srcEnv);
        final BUChanges<S, L, D, IResolutionPath<S, L, D>> changes =
                BUChanges.ofPaths(srcEnv, _env.pathSet()).flatMap((k, ps) -> {
                    final List<IResolutionPath<S, L, D>> newPs = ps.stream()
                            .flatMap(
                                    p -> (srcEnv.kind.arePathsRelevant() ? ofOpt(Paths.append(step, p)) : Stream.of(p)))
                            .collect(Collectors.toList());
                    return Tuple2.of(pathKey(k.name(), EdgeOrData.edge(step.getLabel())), newPs);
                });
        logger.trace("queued back changes {} to {} added {} removed {}", srcEnv, dstEnv, changes.addedPaths().paths(),
                changes.removedPaths().paths());
        queueChanges(dstEnv, changes);
        logger.trace("added back edge {} {} {}", dstEnv, step, srcEnv);
    }

    private boolean isClosed(S scope, EdgeOrData<L> label) {
        return isClosed.test(scope, label);
    }


    private BUEnvKey<S, L, D> envKey(BUEnvKind<L, D> kind, S scope, IRegExpMatcher<L> wf) {
        final Tuple3<BUEnvKind<L, D>, S, IRegExp<L>> key = Tuple3.of(kind, scope, wf.regexp());
        BUEnvKey<S, L, D> result;
        if((result = envKeys.get(key)) == null) {
            envKeys.put(key, result = new BUEnvKey<>(kind, scope, wf));
        }
        return result;
    }

    private BUPathKey<L> pathKey(SpacedName name, EdgeOrData<L> label) {
        final Tuple2<SpacedName, EdgeOrData<L>> key = Tuple2.of(name, label);
        BUPathKey<L> result;
        if((result = pathKeys.get(key)) == null) {
            pathKeys.put(key, result = new BUPathKey<>(name, label));
        }
        return result;
    }


    private boolean isComplete(BUEnvKey<S, L, D> env) {
        final BUEnvKey<S, L, D> rep = sccGraph.getRepresentative(env);
        // check if the component still depends on other components
        if(sccGraph.hasOutgoingEdges(rep)) {
            return false;
        }
        // check if there are pending changes for any environment in the component
        for(BUEnvKey<S, L, D> cenv : sccGraph.sccs.getPartition(rep)) {
            if(pendingChanges.contains(cenv) || openEdges.containsKey(cenv)) {
                return false;
            }
        }
        return true;
    }

    private void throwIfIncomplete(BUEnvKey<S, L, D> env) throws StuckException, IncompleteException {
        if(!pendingChanges.isEmpty()) {
            throw new IllegalStateException("pending changes should be empty");
        }

        // check for critical edges
        final java.util.Set<BUEnvKey<S, L, D>> reachableEnvs = sccGraph.getAllReachableTargets(env);
        final Set.Transient<CriticalEdge> ces = Set.Transient.of();
        ces.__insertAll(openEdges.get(env));
        for(BUEnvKey<S, L, D> reachEnv : reachableEnvs) {
            ces.__insertAll(openEdges.get(reachEnv));
        }
        if(!ces.isEmpty()) {
            throw new UnsupportedOperationException();
            //            throw new IncompleteException(ces.freeze());
        }
    }


    private static <X> Stream<X> ofOpt(Optional<X> xOrNull) {
        return Streams.stream(xOrNull);
    }

}