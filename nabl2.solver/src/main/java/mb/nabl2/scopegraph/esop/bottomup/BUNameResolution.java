package mb.nabl2.scopegraph.esop.bottomup;

import java.io.Serializable;
import java.util.Collection;
import java.util.Deque;
import java.util.Map.Entry;
import java.util.Objects;
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
import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.relations.IRelation;
import mb.nabl2.relations.RelationDescription;
import mb.nabl2.relations.impl.Relation;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.INameResolution;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.esop.CriticalEdgeException;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.path.IStep;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.ResolutionParameters;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.util.Tuple3;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.nabl2.util.collections.MultiSet;

public class BUNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements INameResolution<S, L, O> {

    @SuppressWarnings("unused") private static final ILogger logger = LoggerUtils.logger(BUNameResolution.class);

    private static enum Kind {
        VISIBLE, // top-level and import paths are ordered
        REACHABLE // top-level paths are unordered, import paths are ordered
    }

    private final IEsopScopeGraph<S, L, O, ?> scopeGraph;
    private final L dataLabel;
    private final Iterable<L> edgeLabels;
    private final IRegExpMatcher<L> wf;
    private final ImmutableMap<Kind, BUFirstStepComparator<S, L, O>> orders;
    private final Predicate2<S, L> isOpen;


    public BUNameResolution(IEsopScopeGraph<S, L, O, ?> scopeGraph, Iterable<L> edgeLabels, L dataLabel, IRegExp<L> wf,
            IRelation<L> order, Predicate2<S, L> isOpen) {
        this.scopeGraph = scopeGraph;
        this.edgeLabels = edgeLabels;
        this.dataLabel = dataLabel;
        this.wf = RegExpMatcher.create(wf);
        final IRelation<L> noOrder = Relation.Immutable.of(RelationDescription.STRICT_PARTIAL_ORDER);
        // @formatter:off
        this.orders = ImmutableMap.of(
            Kind.VISIBLE,   new BUFirstStepComparator<>(dataLabel, order),
            Kind.REACHABLE, new BUFirstStepComparator<>(dataLabel, noOrder)
        );
        // @formatter:on
        this.isOpen = isOpen;
    }

    public static BUNameResolution<Scope, Label, Occurrence> of(IEsopScopeGraph<Scope, Label, Occurrence, ?> scopeGraph,
            ResolutionParameters params, Predicate2<Scope, Label> isOpen) {
        return new BUNameResolution<>(scopeGraph, params.getLabels(), params.getLabelD(), params.getPathWf(),
                params.getSpecificityOrder(), isOpen);
    }

    ///////////////////////////////////////////////////////////////////////////
    // INameResolution
    ///////////////////////////////////////////////////////////////////////////

    @Override public java.util.Set<O> getResolvedRefs() {
        throw new UnsupportedOperationException();
    }

    @Override public Collection<IResolutionPath<S, L, O>> resolve(O ref)
            throws InterruptedException, CriticalEdgeException {
        return resolveRef(ref);
    }

    @Override public Collection<O> decls(S scope) {
        return scopeGraph.getDecls().inverse().get(scope);
    }

    @Override public Collection<O> refs(S scope) {
        return scopeGraph.getRefs().inverse().get(scope);
    }

    @Override public Collection<O> visible(S scope) throws InterruptedException, CriticalEdgeException {
        return Paths.declPathsToDecls(visibleEnv(scope));
    }

    @Override public Collection<O> reachable(S scope) throws InterruptedException, CriticalEdgeException {
        return Paths.declPathsToDecls(reachableEnv(scope));
    }

    @Override public Collection<Map.Entry<O, Collection<IResolutionPath<S, L, O>>>> resolutionEntries() {
        throw new UnsupportedOperationException();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation
    ///////////////////////////////////////////////////////////////////////////

    private Map.Transient<Key, BUEnv<S, L, O, IDeclPath<S, L, O>>> envs = Map.Transient.of();
    private final Deque<InterruptibleRunnable> worklist = Queues.newArrayDeque();
    private final MultiSet.Transient<Key> pendingChanges = MultiSet.Transient.of();
    private final Graph<Key> depGraph = new Graph<>();
    private final IncSCCAlg<Key> sccGraph = new IncSCCAlg<>(depGraph);
    private IRelation3.Transient<Key, IStep<S, L, O>, Key> backedges = HashTrieRelation3.Transient.of();
    private IRelation3.Transient<Key, Tuple3<L, O, IRegExpMatcher<L>>, Key> backimports =
            HashTrieRelation3.Transient.of();

    private Collection<IResolutionPath<S, L, O>> resolveRef(O ref) throws InterruptedException, CriticalEdgeException {
        final S scope;
        if((scope = scopeGraph.getRefs().get(ref).orElse(null)) == null) {
            return Set.Immutable.of();
        }
        final Key key = new Key(Kind.VISIBLE, scope, wf);
        final BUEnv<S, L, O, IDeclPath<S, L, O>> env = getOrCompute(key);
        return env.get(ref.getSpacedName()).stream().flatMap(p -> ofOpt(Paths.resolve(ref, p)))
                .collect(CapsuleCollectors.toSet());
    }

    private Collection<IDeclPath<S, L, O>> visibleEnv(S scope) throws InterruptedException, CriticalEdgeException {
        final Key key = new Key(Kind.VISIBLE, scope, wf);
        return getOrCompute(key).pathSet();
    }

    private Collection<IDeclPath<S, L, O>> reachableEnv(S scope) throws InterruptedException, CriticalEdgeException {
        final Key key = new Key(Kind.REACHABLE, scope, wf);
        return getOrCompute(key).pathSet();
    }

    /**
     * Get or compute the complete environment for the given key. If the environment is incomplete, throws a critical
     * edge exception.
     */
    private BUEnv<S, L, O, IDeclPath<S, L, O>> getOrCompute(Key env)
            throws InterruptedException, CriticalEdgeException {
        final BUEnv<S, L, O, IDeclPath<S, L, O>> _env = init(env);
        while(!worklist.isEmpty()) {
            worklist.pop().run();
        }
        if(!isComplete(env)) {
            // FIXME Include actual critical edges
            logger.warn("incorrect critical edges will be reported");
            throw new CriticalEdgeException(env.scope, dataLabel);
        }
        return _env;
    }

    private BUEnv<S, L, O, IDeclPath<S, L, O>> init(Key env) {
        if(envs.containsKey(env)) {
            return envs.get(env);
        }
        final BUEnv<S, L, O, IDeclPath<S, L, O>> _env = new BUEnv<>(orders.get(env.kind)::compare);
        envs.__put(env, _env);
        depGraph.insertNode(env);
        final Set.Transient<IDeclPath<S, L, O>> declPaths = Set.Transient.of();
        if(env.wf.isAccepting()) {
            if(isOpen.test(env.scope, dataLabel)) {
                // FIXME mark env/dataLabel open
                logger.warn("ignoring open edge {}/{}", env.scope, dataLabel);
            } else {
                for(O d : scopeGraph.getDecls().inverse().get(env.scope)) {
                    declPaths.__insert(Paths.decl(Paths.<S, L, O>empty(env.scope), d));
                }
            }
        }
        for(L l : edgeLabels) {
            IRegExpMatcher<L> nextWf = env.wf.match(l);
            if(nextWf.isEmpty()) {
                continue;
            }
            if(isOpen.test(env.scope, l)) {
                // FIXME mark env/l open
                logger.warn("ignoring open edge {}/{}", env.scope, l);
            } else {
                for(S scope : scopeGraph.getDirectEdges().get(env.scope, l)) {
                    final Key srcEnv = new Key(env.kind, scope, nextWf);
                    init(srcEnv);
                    addBackEdge(srcEnv, Paths.direct(env.scope, l, srcEnv.scope), env);
                }
                for(O ref : scopeGraph.getImportEdges().get(env.scope, l)) {
                    addBackImport(ref, l, nextWf, env);
                }
            }
        }
        queueChanges(env, new BUChanges<>(declPaths.freeze(), Set.Immutable.of()));
        return _env;
    }

    private void queueChanges(Key env, BUChanges<S, L, O, IDeclPath<S, L, O>> changes) {
        pendingChanges.add(env);
        worklist.add(() -> processChanges(env, changes));
    }

    private void processChanges(Key env, BUChanges<S, L, O, IDeclPath<S, L, O>> changes) throws InterruptedException {
        pendingChanges.remove(env);
        final BUChanges<S, L, O, IDeclPath<S, L, O>> newChanges = envs.get(env).apply(changes);
        for(Entry<IStep<S, L, O>, Key> entry : backedges.get(env)) {
            final Key dstEnv = entry.getValue();
            final IStep<S, L, O> step = entry.getKey();
            final BUChanges<S, L, O, IDeclPath<S, L, O>> envChanges =
                    newChanges.flatMap(p -> ofOpt(Paths.append(step, p)), p -> ofOpt(Paths.append(step, p)));
            queueChanges(dstEnv, envChanges);
        }
        if(isComplete(env)) {
            for(Entry<IStep<S, L, O>, Key> entry : backedges.get(env)) {
                final Key dstEnv = entry.getValue();
                depGraph.deleteEdgeThatExists(dstEnv, env);
            }
            for(Entry<Tuple3<L, O, IRegExpMatcher<L>>, Key> entry : backimports.get(env)) {
                final Key dstEnv = entry.getValue();
                final Tuple3<L, O, IRegExpMatcher<L>> _st = entry.getKey();
                addImportBackEdges(env, _st, dstEnv);
                depGraph.deleteEdgeThatExists(dstEnv, env);
            }
            backedges.remove(env);
            backimports.remove(env);
        }
    }

    private boolean isComplete(Key env) {
        final Key rep = sccGraph.getRepresentative(env);
        // check if the component still depends on other components
        if(sccGraph.hasOutgoingEdges(rep)) {
            return false;
        }
        // check if there are pending changes for any environment in the component, and if any backimports are unresolved
        for(Key cenv : sccGraph.sccs.getPartition(rep)) {
            if(pendingChanges.contains(cenv) || backimports.inverse().contains(cenv)) {
                return false;
            }
        }
        return true;
    }

    private void addBackEdge(Key srcEnv, IStep<S, L, O> st, Key dstEnv) {
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
        final Set.Immutable<IDeclPath<S, L, O>> paths = envs.get(srcEnv).pathSet().stream()
                .flatMap(p -> ofOpt(Paths.append(st, p))).collect(CapsuleCollectors.toSet());
        logger.trace(" * paths {}", paths);
        queueChanges(dstEnv, new BUChanges<>(paths, Set.Immutable.of()));
    }

    private void addBackImport(O ref, L l, IRegExpMatcher<L> wf, Key dstEnv) {
        final S refScope;
        if((refScope = scopeGraph.getRefs().get(ref).orElse(null)) == null) {
            return;
        }
        final Key refEnv = new Key(Kind.VISIBLE, refScope, this.wf);
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

    private void addImportBackEdges(Key refEnv, Tuple3<L, O, IRegExpMatcher<L>> _st, Key dstEnv) {
        logger.trace("add import back edges {}<~{}~{}", dstEnv, _st, refEnv);
        final L l = _st._1();
        final O ref = _st._2();
        final IRegExpMatcher<L> wf = _st._3();
        final Set.Immutable<IResolutionPath<S, L, O>> paths =
                envs.get(refEnv).get(ref.getSpacedName()).stream().flatMap(p -> {
                    return ofOpt(Paths.resolve(ref, p));
                }).collect(CapsuleCollectors.toSet());
        logger.trace(" * paths {}", paths);
        for(IResolutionPath<S, L, O> p : paths) {
            scopeGraph.getExportEdges().get(p.getDeclaration(), l).forEach(ss -> {
                final Key srcEnv = new Key(dstEnv.kind, ss, wf);
                init(srcEnv);
                final IStep<S, L, O> st = Paths.named(dstEnv.scope, l, p, srcEnv.scope);
                addBackEdge(srcEnv, st, dstEnv);
            });
        }
    }


    private class Key implements Serializable {
        private static final long serialVersionUID = 1L;

        public final Kind kind;
        public final S scope;
        public final IRegExpMatcher<L> wf;

        public Key(Kind kind, S scope, IRegExpMatcher<L> wf) {
            this.kind = kind;
            this.scope = scope;
            this.wf = wf;
            if(wf.isEmpty()) {
                throw new AssertionError();
            }
        }

        @Override public int hashCode() {
            return Objects.hash(kind, scope, wf.regexp());
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") Key other = (Key) obj;
            return kind.equals(other.kind) && scope.equals(other.scope) && wf.regexp().equals(other.wf.regexp());
        }

        @Override public String toString() {
            return kind.toString() + "/" + scope.toString() + "/" + wf.regexp().toString();
        }

    }


    private static <X> Stream<X> ofOpt(Optional<X> xOrNull) {
        return Streams.stream(xOrNull);
    }

}