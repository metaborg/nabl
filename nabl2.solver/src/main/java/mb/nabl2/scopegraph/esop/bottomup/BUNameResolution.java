package mb.nabl2.scopegraph.esop.bottomup;

import java.util.Collection;
import java.util.Deque;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.viatra.query.runtime.base.itc.alg.incscc.IncSCCAlg;
import org.eclipse.viatra.query.runtime.base.itc.graphimpl.Graph;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Queues;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.relations.IRelation;
import mb.nabl2.relations.RelationDescription;
import mb.nabl2.relations.impl.Relation;
import mb.nabl2.scopegraph.CriticalEdgeException;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IResolutionParameters;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.StuckException;
import mb.nabl2.scopegraph.esop.CriticalEdge;
import mb.nabl2.scopegraph.esop.IEsopNameResolution;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.path.IStep;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.nabl2.util.collections.HashTrieRelation2;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation2;
import mb.nabl2.util.collections.IRelation3;
import mb.nabl2.util.collections.MultiSet;

public class BUNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution<S, L, O> {

    @SuppressWarnings("unused") private static final ILogger logger = LoggerUtils.logger(BUNameResolution.class);

    private final IEsopScopeGraph<S, L, O, ?> scopeGraph;
    private final L dataLabel;
    private final Iterable<L> edgeLabels;
    private final IRegExpMatcher<L> wf;
    private final ImmutableMap<BUEnvKind, BULabelOrder<L>> orders;
    private final Predicate2<S, L> isClosed;


    public BUNameResolution(IResolutionParameters<L> params, IEsopScopeGraph<S, L, O, ?> scopeGraph,
            Predicate2<S, L> isClosed) {
        this.scopeGraph = scopeGraph;
        this.edgeLabels = params.getLabels();
        this.dataLabel = params.getLabelD();
        this.wf = RegExpMatcher.create(params.getPathWf());
        final IRelation<L> noOrder = Relation.Immutable.of(RelationDescription.STRICT_PARTIAL_ORDER);
        // @formatter:off
        this.orders = ImmutableMap.of(
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
            _cache.envs.forEach((env, ps) -> nr.envs.__put(env, new BUEnv<>(nr.orders.get(env.kind), ps.melt())));
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

    @Override public Collection<IResolutionPath<S, L, O>> resolve(O ref)
            throws InterruptedException, CriticalEdgeException, StuckException {
        return resolveRef(ref);
    }

    @Override public Collection<O> decls(S scope) {
        return scopeGraph.getDecls().inverse().get(scope);
    }

    @Override public Collection<O> refs(S scope) {
        return scopeGraph.getRefs().inverse().get(scope);
    }

    @Override public Collection<O> visible(S scope) throws InterruptedException, CriticalEdgeException, StuckException {
        return Paths.declPathsToDecls(visibleEnv(scope));
    }

    @Override public Collection<O> reachable(S scope)
            throws InterruptedException, CriticalEdgeException, StuckException {
        return Paths.declPathsToDecls(reachableEnv(scope));
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
        return new BUCache<>(envs, completed, backedges, backimports, openEdges);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation
    ///////////////////////////////////////////////////////////////////////////

    private final Map.Transient<BUEnvKey<S, L>, BUEnv<S, L, O, IDeclPath<S, L, O>>> envs = Map.Transient.of();
    private final Set.Transient<BUEnvKey<S, L>> completed = Set.Transient.of();
    private final IRelation2.Transient<BUEnvKey<S, L>, CriticalEdge> openEdges = HashTrieRelation2.Transient.of();
    private final IRelation3.Transient<BUEnvKey<S, L>, IStep<S, L, O>, BUEnvKey<S, L>> backedges =
            HashTrieRelation3.Transient.of();
    private final IRelation3.Transient<BUEnvKey<S, L>, Tuple3<L, O, IRegExpMatcher<L>>, BUEnvKey<S, L>> backimports =
            HashTrieRelation3.Transient.of();

    private final Deque<InterruptibleRunnable> worklist = Queues.newArrayDeque();
    private final MultiSet.Transient<BUEnvKey<S, L>> pendingChanges = MultiSet.Transient.of();
    private final Graph<BUEnvKey<S, L>> depGraph = new Graph<>();
    private final IncSCCAlg<BUEnvKey<S, L>> sccGraph = new IncSCCAlg<>(depGraph);

    private Collection<IResolutionPath<S, L, O>> resolveRef(O ref)
            throws InterruptedException, CriticalEdgeException, StuckException {
        final S scope;
        if((scope = scopeGraph.getRefs().get(ref).orElse(null)) == null) {
            return Set.Immutable.of();
        }
        final BUEnvKey<S, L> key = new BUEnvKey<>(BUEnvKind.VISIBLE, scope, wf);
        final BUEnv<S, L, O, IDeclPath<S, L, O>> env = getOrCompute(key);
        return env.paths(ref.getSpacedName()).stream().flatMap(p -> ofOpt(Paths.resolve(ref, p)))
                .collect(CapsuleCollectors.toSet());
    }

    private Collection<IDeclPath<S, L, O>> visibleEnv(S scope)
            throws InterruptedException, CriticalEdgeException, StuckException {
        final BUEnvKey<S, L> key = new BUEnvKey<>(BUEnvKind.VISIBLE, scope, wf);
        return getOrCompute(key).paths();
    }

    private Collection<IDeclPath<S, L, O>> reachableEnv(S scope)
            throws InterruptedException, CriticalEdgeException, StuckException {
        final BUEnvKey<S, L> key = new BUEnvKey<>(BUEnvKind.REACHABLE, scope, wf);
        return getOrCompute(key).paths();
    }

    /**
     * Get or compute the complete environment for the given key. If the environment is incomplete, throws a critical
     * edge exception.
     */
    private BUEnv<S, L, O, IDeclPath<S, L, O>> getOrCompute(BUEnvKey<S, L> env)
            throws InterruptedException, CriticalEdgeException, StuckException {
        //        logger.info("compute {}", env);
        final BUEnv<S, L, O, IDeclPath<S, L, O>> _env = init(env);
        work();
        throwIfIncomplete(env);
        return _env;
    }

    @SuppressWarnings("unchecked") @Override public void update(Iterable<CriticalEdge> criticalEdges)
            throws InterruptedException {
        //        logger.info("update {}", criticalEdges);
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
        work();
    }

    private void work() throws InterruptedException {
        while(!worklist.isEmpty()) {
            worklist.pop().run();
        }
    }

    private BUEnv<S, L, O, IDeclPath<S, L, O>> init(BUEnvKey<S, L> env) {
        BUEnv<S, L, O, IDeclPath<S, L, O>> _env = envs.get(env);
        if(_env != null) {
            return _env;
        }
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
        queueChanges(env, new BUChanges<>(declPaths.freeze(), BUPathSet.Immutable.of()));
        queueCheckComplete(env);
        return _env;
    }

    private void initData(BUEnvKey<S, L> env, BUPathSet.Transient<S, L, O, IDeclPath<S, L, O>> declPaths) {
        for(O d : scopeGraph.getDecls().inverse().get(env.scope)) {
            declPaths.add(d.getSpacedName(), dataLabel, Paths.decl(Paths.<S, L, O>empty(env.scope), d));
        }
    }

    private void initEdge(BUEnvKey<S, L> env, L l, IRegExpMatcher<L> nextWf) {
        for(S scope : scopeGraph.getDirectEdges().get(env.scope, l)) {
            final BUEnvKey<S, L> srcEnv = new BUEnvKey<>(env.kind, scope, nextWf);
            init(srcEnv);
            addBackEdge(srcEnv, Paths.direct(env.scope, l, srcEnv.scope), env);
        }
        for(O ref : scopeGraph.getImportEdges().get(env.scope, l)) {
            addBackImport(ref, l, nextWf, env);
        }
    }

    private void queueChanges(BUEnvKey<S, L> env, BUChanges<S, L, O, IDeclPath<S, L, O>> changes) {
        if(completed.contains(env)) {
            throw new IllegalStateException();
        }
        if(changes.isEmpty()) {
            return;
        }
        pendingChanges.add(env);
        worklist.add(() -> processChanges(env, changes));
    }

    private void processChanges(BUEnvKey<S, L> env, BUChanges<S, L, O, IDeclPath<S, L, O>> changes)
            throws InterruptedException {
        if(completed.contains(env)) {
            throw new IllegalStateException();
        }
        pendingChanges.remove(env);
        final BUEnv<S, L, O, IDeclPath<S, L, O>> _env = envs.get(env);
        //        logger.info("{}: adding {}, removing {}", env, changes.addedPaths().paths(), changes.removedPaths().paths());
        _env.apply(changes);
        if(true && _env.hasChanges() && !pendingChanges.contains(env)) {
            final BUChanges<S, L, O, IDeclPath<S, L, O>> newChanges = _env.commit();
            //            logger.info("{}: added {}, removed {}", env, newChanges.addedPaths().paths(), newChanges.removedPaths().paths());
            for(Entry<IStep<S, L, O>, BUEnvKey<S, L>> entry : backedges.get(env)) {
                final BUEnvKey<S, L> dstEnv = entry.getValue();
                final IStep<S, L, O> step = entry.getKey();
                final BUChanges<S, L, O, IDeclPath<S, L, O>> envChanges =
                        newChanges.flatMap(p -> ofOpt(Paths.append(step, p))
                                .map(p2 -> Tuple3.of(p.getDeclaration().getSpacedName(), step.getLabel(), p2)));
                queueChanges(dstEnv, envChanges);
            }
        }
        checkComplete(env);
    }

    private void queueCheckComplete(BUEnvKey<S, L> env) {
        if(completed.contains(env)) {
            return;
        }
        worklist.add(() -> checkComplete(env));
    }

    private void checkComplete(BUEnvKey<S, L> env) throws InterruptedException {
        if(completed.contains(env)) {
            return;
        }
        if(isComplete(env)) {
            completed.__insert(env);
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

    private void addBackEdge(BUEnvKey<S, L> srcEnv, IStep<S, L, O> st, BUEnvKey<S, L> dstEnv) {
        logger.trace("add back edge {}<~{}~{}", dstEnv, st, srcEnv);
        if(!isComplete(srcEnv)) {
            if(backedges.put(srcEnv, st, dstEnv)) {
                depGraph.insertEdge(dstEnv, srcEnv);
                logger.trace(" * new edge");
            } else {
                logger.trace(" * duplicate edge");
                return;
            }
        } else {
            logger.trace(" * complete edge");
        }

        final BUChanges<S, L, O, IDeclPath<S, L, O>> changes =
                envs.get(srcEnv).asChanges().flatMap(p -> ofOpt(Paths.append(st, p))
                        .map(p2 -> Tuple3.of(p.getDeclaration().getSpacedName(), st.getLabel(), p2)));
        logger.trace(" * paths {}", changes);
        queueChanges(dstEnv, changes);
    }

    private void addBackImport(O ref, L l, IRegExpMatcher<L> wf, BUEnvKey<S, L> dstEnv) {
        final S refScope;
        if((refScope = scopeGraph.getRefs().get(ref).orElse(null)) == null) {
            return;
        }
        final BUEnvKey<S, L> refEnv = new BUEnvKey<>(BUEnvKind.VISIBLE, refScope, this.wf);
        init(refEnv);
        final Tuple3<L, O, IRegExpMatcher<L>> _st = Tuple3.of(l, ref, wf);
        logger.trace("add back import {}<~{}~{}", dstEnv, _st, refEnv);
        if(!isComplete(refEnv)) {
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
    }

    private void addImportBackEdges(BUEnvKey<S, L> refEnv, Tuple3<L, O, IRegExpMatcher<L>> _st, BUEnvKey<S, L> dstEnv) {
        logger.trace("add import back edges {}<~{}~{}", dstEnv, _st, refEnv);
        final L l = _st._1();
        final O ref = _st._2();
        final IRegExpMatcher<L> wf = _st._3();
        final Set.Immutable<IResolutionPath<S, L, O>> paths =
                envs.get(refEnv).paths(ref.getSpacedName()).stream().flatMap(p -> {
                    return ofOpt(Paths.resolve(ref, p));
                }).collect(CapsuleCollectors.toSet());
        logger.trace(" * paths {}", paths);
        for(IResolutionPath<S, L, O> p : paths) {
            scopeGraph.getExportEdges().get(p.getDeclaration(), l).forEach(ss -> {
                final BUEnvKey<S, L> srcEnv = new BUEnvKey<>(dstEnv.kind, ss, wf);
                init(srcEnv);
                final IStep<S, L, O> st = Paths.named(dstEnv.scope, l, p, srcEnv.scope);
                addBackEdge(srcEnv, st, dstEnv);
            });
        }
    }


    private boolean isClosed(S scope, L label) {
        return isClosed.test(scope, label) && !scopeGraph.isOpen(scope, label);
    }


    private boolean isComplete(BUEnvKey<S, L> env) {
        final BUEnvKey<S, L> rep = sccGraph.getRepresentative(env);
        // check for critical edges
        if(openEdges.containsKey(env)) {
            return false;
        }
        for(BUEnvKey<S, L> reachEnv : sccGraph.getAllReachableTargets(env)) {
            if(openEdges.containsKey(reachEnv)) {
                return false;
            }
        }
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
        final Set.Transient<CriticalEdge> ces = Set.Transient.of();
        ces.__insertAll(openEdges.get(env));
        for(BUEnvKey<S, L> reachEnv : sccGraph.getAllReachableTargets(env)) {
            ces.__insertAll(openEdges.get(reachEnv));
        }
        if(!ces.isEmpty()) {
            throw new CriticalEdgeException(ces.freeze());
        }

        // check for stuckness
        final BUEnvKey<S, L> rep = sccGraph.getRepresentative(env);
        for(BUEnvKey<S, L> reachRep : sccGraph.getAllReachableTargets(rep)) {
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


    private static <X> Stream<X> ofOpt(Optional<X> xOrNull) {
        return Streams.stream(xOrNull);
    }

}