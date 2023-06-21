package mb.scopegraph.pepm16.bottomup;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;

import org.metaborg.util.collection.HashTrieRelation2;
import org.metaborg.util.collection.HashTrieRelation3;
import org.metaborg.util.collection.IRelation2;
import org.metaborg.util.collection.IRelation3;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.stream.StreamUtil;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.tuple.Tuple3;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.util.graph.alg.incscc.IncSCCAlg;
import mb.nabl2.util.graph.graphimpl.Graph;
import mb.scopegraph.pepm16.CriticalEdgeException;
import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IResolutionParameters;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.StuckException;
import mb.scopegraph.pepm16.esop15.CriticalEdge;
import mb.scopegraph.pepm16.esop15.IEsopNameResolution;
import mb.scopegraph.pepm16.esop15.IEsopScopeGraph;
import mb.scopegraph.pepm16.path.IDeclPath;
import mb.scopegraph.pepm16.path.IResolutionPath;
import mb.scopegraph.pepm16.path.IStep;
import mb.scopegraph.pepm16.terms.SpacedName;
import mb.scopegraph.pepm16.terms.path.Paths;
import mb.scopegraph.regexp.IRegExp;
import mb.scopegraph.regexp.IRegExpMatcher;
import mb.scopegraph.regexp.RegExpMatcher;
import mb.scopegraph.relations.IRelation;
import mb.scopegraph.relations.RelationDescription;
import mb.scopegraph.relations.impl.Relation;

public class BUNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution<S, L, O> {

    @SuppressWarnings("unused") private static final ILogger logger = LoggerUtils.logger(BUNameResolution.class);

    private final IResolutionParameters<L> params;
    private final IEsopScopeGraph<S, L, O, ?> scopeGraph;
    private final L dataLabel;
    private final Iterable<L> edgeLabels;
    private final IRegExpMatcher<L> wf;
    private final Map.Immutable<BUEnvKind, BULabelOrder<L>> orders;
    private final Predicate2<S, L> isClosed;

    private final HashMap<S, Collection<O>> visibles = new HashMap<>();
    private final HashMap<S, Collection<O>> reachables = new HashMap<>();

    public BUNameResolution(IResolutionParameters<L> params, IEsopScopeGraph<S, L, O, ?> scopeGraph,
            Predicate2<S, L> isClosed) {
        this.params = params;
        this.scopeGraph = scopeGraph;
        this.edgeLabels = this.params.getLabels();
        this.dataLabel = this.params.getLabelD();
        this.wf = RegExpMatcher.create(params.getPathWf());
        final IRelation<L> noOrder = Relation.Immutable.of(RelationDescription.STRICT_PARTIAL_ORDER);
        // @formatter:off
        this.orders = Map.Immutable.of(
            BUEnvKind.VISIBLE,   new BULabelOrder<>(params.getSpecificityOrder()),
            BUEnvKind.REACHABLE, new BULabelOrder<>(noOrder)
        );
        // @formatter:on
        this.isClosed = isClosed;
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> BUNameResolution<S, L, O> of(
            IResolutionParameters<L> params, IEsopScopeGraph<S, L, O, ?> scopeGraph, Predicate2<S, L> isClosed,
            IEsopNameResolution.IResolutionCache<S, L, O> cache) {
        final BUNameResolution<S, L, O> nr = new BUNameResolution<>(params, scopeGraph, isClosed);
        if(cache instanceof BUCache) {
            final BUCache<S, L, O> _cache = (BUCache<S, L, O>) cache;
            nr.envKeys.__putAll(_cache.envKeys);
            nr.pathKeys.__putAll(_cache.pathKeys);
            _cache.envs.forEach((env, ps) -> nr.envs.__put(env, new BUEnv<>(nr.orders.get(env.kind), ps)));
            nr.envs.keySet().forEach(e -> nr.depGraph.insertNode(e));
            nr.completed.__insertAll(_cache.completed);
            nr.backedges.putAll(_cache.backedges);
            nr.backedges.stream().forEach(be -> nr.depGraph.insertEdge(be._3(), be._1()));
            nr.backimports.putAll(_cache.backimports);
            nr.backimports.stream().forEach(be -> nr.depGraph.insertEdge(be._3(), be._1()));
            nr.openEdges.putAll(_cache.openEdges);
        }
        return nr;
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> BUNameResolution<S, L, O>
            of(IResolutionParameters<L> params, IEsopScopeGraph<S, L, O, ?> scopeGraph, Predicate2<S, L> isClosed) {
        return new BUNameResolution<>(params, scopeGraph, isClosed);
    }

    ///////////////////////////////////////////////////////////////////////////
    // INameResolution
    ///////////////////////////////////////////////////////////////////////////

    @Override public java.util.Set<O> getResolvedRefs() {
        throw new UnsupportedOperationException();
    }

    @Override public Collection<IResolutionPath<S, L, O>> resolve(O ref, ICancel cancel, IProgress progress)
            throws InterruptedException, CriticalEdgeException, StuckException {
        return resolveRef(ref, cancel);
    }

    @Override public Collection<O> decls(S scope) {
        return scopeGraph.getDecls().inverse().get(scope);
    }

    @Override public Collection<O> refs(S scope) {
        return scopeGraph.getRefs().inverse().get(scope);
    }

    @Override public Collection<O> visible(S scope, ICancel cancel, IProgress progress)
            throws InterruptedException, CriticalEdgeException, StuckException {
        Collection<O> decls = visibles.get(scope);
        if(decls == null) {
            decls = Paths.declPathsToDecls(visibleEnv(scope, cancel));
            visibles.put(scope, decls);
        }
        return decls;
    }

    @Override public Collection<O> reachable(S scope, ICancel cancel, IProgress progress)
            throws InterruptedException, CriticalEdgeException, StuckException {
        Collection<O> decls = reachables.get(scope);
        if(decls == null) {
            decls = Paths.declPathsToDecls(reachableEnv(scope, cancel));
            reachables.put(scope, decls);
        }
        return decls;
    }

    @Override public Collection<Map.Entry<O, Collection<IResolutionPath<S, L, O>>>> resolutionEntries() {
        throw new UnsupportedOperationException();
    }

    ///////////////////////////////////////////////////////////////////////////
    // IEsopNameResolution
    ///////////////////////////////////////////////////////////////////////////

    @Override public boolean addCached(IResolutionCache<S, L, O> cache) {
        return false;
    }

    @Override public IResolutionCache<S, L, O> toCache() {
        return new BUCache<>(envKeys, pathKeys, envs, completed, backedges, backimports, openEdges);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation
    ///////////////////////////////////////////////////////////////////////////

    private final Map.Transient<Tuple3<BUEnvKind, S, IRegExp<L>>, BUEnvKey<S, L>> envKeys = Map.Transient.of();
    private final Map.Transient<Tuple2<SpacedName, L>, BUPathKey<L>> pathKeys = Map.Transient.of();
    private final Map.Transient<BUEnvKey<S, L>, BUEnv<S, L, O, IDeclPath<S, L, O>>> envs = Map.Transient.of();
    private final Set.Transient<BUEnvKey<S, L>> completed = Set.Transient.of();
    private final IRelation2.Transient<BUEnvKey<S, L>, CriticalEdge> openEdges = HashTrieRelation2.Transient.of();
    private final IRelation3.Transient<BUEnvKey<S, L>, IStep<S, L, O>, BUEnvKey<S, L>> backedges =
            HashTrieRelation3.Transient.of();
    private final IRelation3.Transient<BUEnvKey<S, L>, Tuple3<L, O, IRegExpMatcher<L>>, BUEnvKey<S, L>> backimports =
            HashTrieRelation3.Transient.of();

    private final Deque<InterruptibleRunnable> worklist = new ArrayDeque<>();
    private final MultiSet.Transient<BUEnvKey<S, L>> pendingChanges = MultiSet.Transient.of();
    private final Graph<BUEnvKey<S, L>> depGraph = new Graph<>();
    private final IncSCCAlg<BUEnvKey<S, L>> sccGraph = new IncSCCAlg<>(depGraph);

    private Collection<IResolutionPath<S, L, O>> resolveRef(O ref, ICancel cancel)
            throws InterruptedException, CriticalEdgeException, StuckException {
        final S scope;
        if((scope = scopeGraph.getRefs().get(ref).orElse(null)) == null) {
            return Set.Immutable.of();
        }
        final BUEnvKey<S, L> key = envKey(BUEnvKind.VISIBLE, scope, wf);
        final BUEnv<S, L, O, IDeclPath<S, L, O>> env = getOrCompute(key, cancel);
        return StreamUtil.filterMap(env.pathSet().paths(ref.getSpacedName()).stream(),
            p -> Paths.resolve(ref, p)).collect(CapsuleCollectors.toSet());
    }

    private Collection<IDeclPath<S, L, O>> visibleEnv(S scope, ICancel cancel)
            throws InterruptedException, CriticalEdgeException, StuckException {
        final BUEnvKey<S, L> key = envKey(BUEnvKind.VISIBLE, scope, wf);
        return getOrCompute(key, cancel).pathSet().paths();
    }

    private Collection<IDeclPath<S, L, O>> reachableEnv(S scope, ICancel cancel)
            throws InterruptedException, CriticalEdgeException, StuckException {
        final BUEnvKey<S, L> key = envKey(BUEnvKind.REACHABLE, scope, wf);
        return getOrCompute(key, cancel).pathSet().paths();
    }

    /**
     * Get or compute the complete environment for the given key. If the environment is incomplete, throws a critical
     * edge exception.
     * 
     * @param cancel
     *            TODO
     */
    private BUEnv<S, L, O, IDeclPath<S, L, O>> getOrCompute(BUEnvKey<S, L> env, ICancel cancel)
            throws InterruptedException, CriticalEdgeException, StuckException {
        final BUEnv<S, L, O, IDeclPath<S, L, O>> _env = init(env);
        work(cancel);
        throwIfIncomplete(env);
        return _env;
    }

    @SuppressWarnings("unchecked") @Override public void update(Iterable<CriticalEdge> criticalEdges, ICancel cancel,
            IProgress progress) throws InterruptedException {
        for(CriticalEdge ce : criticalEdges) {
            if(!isClosed((S) ce.scope(), (L) ce.label())) {
                continue;
            }
            for(BUEnvKey<S, L> env : openEdges.removeValue(ce)) {
                final BUPathSet.Transient<S, L, O, IDeclPath<S, L, O>> declPaths = BUPathSet.Transient.of();
                if(ce.label().equals(dataLabel)) {
                    initData(env, declPaths);
                } else {
                    initEdge(env, (L) ce.label(), env.wf.match((L) ce.label()));
                }
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

    private BUEnv<S, L, O, IDeclPath<S, L, O>> init(BUEnvKey<S, L> env) {
        BUEnv<S, L, O, IDeclPath<S, L, O>> _env = envs.get(env);
        if(_env != null) {
            return _env;
        }
        logger.trace("init {}", env);
        envs.__put(env, _env = new BUEnv<>(orders.get(env.kind)));
        depGraph.insertNode(env);
        final BUPathSet.Transient<S, L, O, IDeclPath<S, L, O>> declPaths = BUPathSet.Transient.of();
        if(env.wf.isAccepting()) {
            if(isClosed(env.scope, dataLabel)) {
                initData(env, declPaths);
            } else {
                openEdges.put(env, CriticalEdge.of(env.scope, dataLabel));
            }
        }
        for(L l : edgeLabels) {
            IRegExpMatcher<L> nextWf = env.wf.match(l);
            if(nextWf.isEmpty()) {
                continue;
            }
            if(isClosed(env.scope, l)) {
                initEdge(env, l, nextWf);
            } else {
                openEdges.put(env, CriticalEdge.of(env.scope, l));
            }
        }
        logger.trace("queued init changes {} added {}", env, declPaths.names());
        queueChanges(env, new BUChanges<>(declPaths.freeze(), BUPathSet.Immutable.of()));
        queueCheckComplete(env);
        return _env;
    }

    private void initData(BUEnvKey<S, L> env, BUPathSet.Transient<S, L, O, IDeclPath<S, L, O>> declPaths) {
        logger.trace("init data {}", env);
        for(O d : scopeGraph.getDecls().inverse().get(env.scope)) {
            final BUPathKey<L> key = pathKey(d.getSpacedName(), dataLabel);
            declPaths.add(key, Paths.decl(Paths.<S, L, O>empty(env.scope), d));
        }
        logger.trace("inited data {}", env);
    }

    private void initEdge(BUEnvKey<S, L> env, L l, IRegExpMatcher<L> nextWf) {
        logger.trace("init edges {} label {} next wf {}", env, l, nextWf);
        for(S scope : scopeGraph.getDirectEdges().get(env.scope, l)) {
            final BUEnvKey<S, L> srcEnv = envKey(env.kind, scope, nextWf);
            init(srcEnv);
            addBackEdge(srcEnv, Paths.direct(env.scope, l, srcEnv.scope), env);
        }
        for(O ref : scopeGraph.getImportEdges().get(env.scope, l)) {
            addBackImport(ref, l, nextWf, env);
        }
        logger.trace("inited edge {} next wf {}", env, nextWf);
    }

    private void queueChanges(BUEnvKey<S, L> env, BUChanges<S, L, O, IDeclPath<S, L, O>> changes) {
        if(completed.contains(env)) {
            throw new IllegalStateException();
        }
        if(changes.isEmpty()) {
            return;
        }
        pendingChanges.add(env);
        worklist.add((cancel) -> processChanges(env, changes));
    }

    private void processChanges(BUEnvKey<S, L> env, BUChanges<S, L, O, IDeclPath<S, L, O>> changes)
            throws InterruptedException {
        if(completed.contains(env)) {
            throw new IllegalStateException();
        }
        pendingChanges.remove(env);
        logger.trace("process changes {} added {} removed {}", env, changes.addedPaths(), changes.removedPaths());
        final BUEnv<S, L, O, IDeclPath<S, L, O>> _env = envs.get(env);
        _env.apply(changes);
        if(_env.hasChanges() && !pendingChanges.contains(env)) {
            final BUChanges<S, L, O, IDeclPath<S, L, O>> newChanges = _env.commitChanges();
            for(Entry<IStep<S, L, O>, BUEnvKey<S, L>> entry : backedges.get(env)) {
                final BUEnvKey<S, L> dstEnv = entry.getValue();
                final IStep<S, L, O> step = entry.getKey();
                final BUChanges<S, L, O, IDeclPath<S, L, O>> envChanges = newChanges.flatMap((k, ps) -> {
                    final ImList.Transient<IDeclPath<S, L, O>> newPs =
                            new ImList.Transient<>(ps.size());
                    for(IDeclPath<S, L, O> p : ps) {
                        (params.getPathRelevance() ? Paths.append(step, p) : Optional.of(p)).ifPresent(newPs::add);
                    }
                    return Tuple2.of(pathKey(k.name(), step.getLabel()), newPs.freeze());
                });
                logger.trace("queued fwd changes {} to {} added {} removed {}", env, dstEnv,
                        envChanges.addedPaths().paths(), envChanges.removedPaths().paths());
                queueChanges(dstEnv, envChanges);
            }
        }
        checkComplete(env);
    }

    private void queueCheckComplete(BUEnvKey<S, L> env) {
        if(completed.contains(env)) {
            return;
        }
        worklist.add((cancel) -> checkComplete(env));
    }

    private void checkComplete(BUEnvKey<S, L> env) throws InterruptedException {
        if(completed.contains(env)) {
            return;
        }
        if(isComplete(env)) {
            completed.__insert(env);
            final BUEnv<S, L, O, IDeclPath<S, L, O>> _env = envs.get(env);
            logger.trace("completed {} {}", env, _env.pathSet());
            for(Entry<IStep<S, L, O>, BUEnvKey<S, L>> entry : backedges.remove(env).entrySet()) {
                final BUEnvKey<S, L> dstEnv = entry.getValue();
                depGraph.deleteEdgeThatExists(dstEnv, env);
                queueCheckComplete(dstEnv);
            }
            for(Entry<Tuple3<L, O, IRegExpMatcher<L>>, BUEnvKey<S, L>> entry : backimports.remove(env).entrySet()) {
                final BUEnvKey<S, L> dstEnv = entry.getValue();
                final Tuple3<L, O, IRegExpMatcher<L>> _st = entry.getKey();
                addImportBackEdges(env, _st, dstEnv);
                depGraph.deleteEdgeThatExists(dstEnv, env);
                queueCheckComplete(dstEnv);
            }
        }
    }

    private void addBackEdge(BUEnvKey<S, L> srcEnv, IStep<S, L, O> step, BUEnvKey<S, L> dstEnv) {
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

        final BUEnv<S, L, O, IDeclPath<S, L, O>> _env = envs.get(srcEnv);
        final BUChanges<S, L, O, IDeclPath<S, L, O>> changes =
                BUChanges.ofPaths(srcEnv, _env.pathSet()).flatMap((k, ps) -> {
                    final ImList.Transient<IDeclPath<S, L, O>> newPs =
                            new ImList.Transient<>(ps.size());
                    for(IDeclPath<S, L, O> p : ps) {
                        (params.getPathRelevance() ? Paths.append(step, p) : Optional.of(p)).ifPresent(newPs::add);
                    }
                    return Tuple2.of(pathKey(k.name(), step.getLabel()), newPs.freeze());
                });
        logger.trace("queued back changes {} to {} added {} removed {}", srcEnv, dstEnv, changes.addedPaths().paths(),
                changes.removedPaths().paths());
        queueChanges(dstEnv, changes);
        logger.trace("added back edge {} {} {}", dstEnv, step, srcEnv);
    }

    private void addBackImport(O ref, L l, IRegExpMatcher<L> wf, BUEnvKey<S, L> dstEnv) {
        final S refScope;
        if((refScope = scopeGraph.getRefs().get(ref).orElse(null)) == null) {
            return;
        }
        final BUEnvKey<S, L> refEnv = envKey(BUEnvKind.VISIBLE, refScope, this.wf);
        init(refEnv);
        final Tuple3<L, O, IRegExpMatcher<L>> _st = Tuple3.of(l, ref, wf);
        logger.trace("add back import {} {} {}", dstEnv, _st, refEnv);
        if(!completed.contains(refEnv)) {
            if(backimports.put(refEnv, _st, dstEnv)) {
                depGraph.insertEdge(dstEnv, refEnv);
                logger.trace(" * new import");
            } else {
                logger.trace(" * duplicate import");
            }
        } else {
            logger.trace(" * complete import");
            addImportBackEdges(refEnv, _st, dstEnv);
        }
        logger.trace("added back import {} {} {}", dstEnv, _st, refEnv);
    }

    private void addImportBackEdges(BUEnvKey<S, L> refEnv, Tuple3<L, O, IRegExpMatcher<L>> _st, BUEnvKey<S, L> dstEnv) {
        if(!completed.contains(refEnv)) {
            throw new IllegalStateException();
        }
        logger.trace("add import back edges {} {} {}", dstEnv, _st, refEnv);
        final L l = _st._1();
        final O ref = _st._2();
        final IRegExpMatcher<L> wf = _st._3();
        final Collection<IDeclPath<S, L, O>> declPaths = envs.get(refEnv).pathSet().paths(ref.getSpacedName());
        logger.trace("import back edges {} decl paths {}", dstEnv, declPaths);
        final Set.Immutable<IResolutionPath<S, L, O>> resPaths =
            StreamUtil.filterMap(declPaths.stream(), p -> Paths.resolve(ref, p))
                .collect(CapsuleCollectors.toSet());
        logger.trace("import back edges {} res paths {}", dstEnv, declPaths);
        logger.trace(" * paths {}", resPaths);
        for(IResolutionPath<S, L, O> p : resPaths) {
            scopeGraph.getExportEdges().get(p.getDeclaration(), l).forEach(ss -> {
                final BUEnvKey<S, L> srcEnv = envKey(dstEnv.kind, ss, wf);
                init(srcEnv);
                final IStep<S, L, O> st = Paths.named(dstEnv.scope, l, p, srcEnv.scope);
                addBackEdge(srcEnv, st, dstEnv);
            });
        }
        logger.trace("added import back edges {} {} {}", dstEnv, _st, refEnv);
    }


    private boolean isClosed(S scope, L label) {
        return isClosed.test(scope, label) && !scopeGraph.isOpen(scope, label);
    }


    private BUEnvKey<S, L> envKey(BUEnvKind kind, S scope, IRegExpMatcher<L> wf) {
        final Tuple3<BUEnvKind, S, IRegExp<L>> key = Tuple3.of(kind, scope, wf.regexp());
        BUEnvKey<S, L> result;
        if((result = envKeys.get(key)) == null) {
            envKeys.__put(key, result = new BUEnvKey<>(kind, scope, wf));
        }
        return result;
    }

    private BUPathKey<L> pathKey(SpacedName name, L label) {
        final Tuple2<SpacedName, L> key = Tuple2.of(name, label);
        BUPathKey<L> result;
        if((result = pathKeys.get(key)) == null) {
            pathKeys.__put(key, result = new BUPathKey<>(name, label));
        }
        return result;
    }


    private boolean isComplete(BUEnvKey<S, L> env) {
        final BUEnvKey<S, L> rep = sccGraph.getRepresentative(env);
        // check if the component still depends on other components
        if(sccGraph.hasOutgoingEdges(rep)) {
            return false;
        }
        // check if there are pending changes for any environment in the component, and if any backimports are unresolved
        for(BUEnvKey<S, L> cenv : sccGraph.sccs.getPartition(rep)) {
            if(pendingChanges.contains(cenv) || openEdges.containsKey(cenv) || backimports.inverse().contains(cenv)) {
                return false;
            }
        }
        return true;
    }

    private void throwIfIncomplete(BUEnvKey<S, L> env) throws StuckException, CriticalEdgeException {
        if(!pendingChanges.isEmpty()) {
            throw new IllegalStateException("pending changes should be empty");
        }

        // check for critical edges
        final java.util.Set<BUEnvKey<S, L>> reachableEnvs = sccGraph.getAllReachableTargets(env);
        final Set.Transient<CriticalEdge> ces = Set.Transient.of();
        ces.__insertAll(openEdges.get(env));
        for(BUEnvKey<S, L> reachEnv : reachableEnvs) {
            ces.__insertAll(openEdges.get(reachEnv));
        }
        if(!ces.isEmpty()) {
            throw new CriticalEdgeException(ces.freeze());
        }

        // check for stuckness
        final java.util.Set<BUEnvKey<S, L>> reachableReps =
                reachableEnvs.stream().map(sccGraph::getRepresentative).collect(CapsuleCollectors.toSet());
        for(BUEnvKey<S, L> reachRep : reachableReps) {
            if(sccGraph.hasOutgoingEdges(reachRep)) {
                continue;
            }
            java.util.Set<BUEnvKey<S, L>> scc = sccGraph.sccs.getPartition(reachRep);
            boolean hasImports = false;
            for(BUEnvKey<S, L> cenv : scc) {
                hasImports |= backimports.inverse().contains(cenv);
            }
            if(!hasImports) {
                continue;
            }
            final Set.Immutable<Tuple3<BUEnvKey<S, L>, L, BUEnvKey<S, L>>> edges =
                    backedges.stream().filter(e -> scc.contains(e._3()))
                            .map(e -> Tuple3.of(e._3(), e._2().getLabel(), e._1())).collect(CapsuleCollectors.toSet());
            final Set.Immutable<Tuple3<BUEnvKey<S, L>, Tuple2<L, O>, BUEnvKey<S, L>>> imports =
                    backimports.stream().filter(e -> scc.contains(e._3()))
                            .map(e -> Tuple3.of(e._3(), Tuple2.of(e._2()._1(), e._2()._2()), e._1()))
                            .collect(CapsuleCollectors.toSet());
            throw new BUStuckException(scc, edges, imports);
        }
    }

}