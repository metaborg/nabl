package mb.scopegraph.pepm16.esop15.lazy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import io.usethesource.capsule.Map;
import mb.scopegraph.pepm16.CriticalEdgeException;
import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IResolutionParameters;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.esop15.IEsopNameResolution;
import mb.scopegraph.pepm16.esop15.IEsopScopeGraph;
import mb.scopegraph.pepm16.esop15.lazy.EsopEnvs.EnvProvider;
import mb.scopegraph.pepm16.esop15.lazy.EsopEnvs.LazyEnv;
import mb.scopegraph.pepm16.path.IDeclPath;
import mb.scopegraph.pepm16.path.IPath;
import mb.scopegraph.pepm16.path.IResolutionPath;
import mb.scopegraph.pepm16.path.IScopePath;
import mb.scopegraph.pepm16.terms.path.Paths;
import mb.scopegraph.regexp.IRegExpMatcher;
import mb.scopegraph.regexp.RegExpMatcher;
import mb.scopegraph.relations.IRelation;
import mb.scopegraph.relations.RelationDescription;
import mb.scopegraph.relations.impl.Relation;

public class EsopNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution<S, L, O> {

    private final IResolutionParameters<L> params;
    private final IEsopScopeGraph<S, L, O, ?> scopeGraph;
    private final Set<L> labels;
    private final L labelD;
    private final L labelR;
    private final IRegExpMatcher<L> wf;
    private final IRelation.Immutable<L> order;
    private final IRelation<L> noOrder;
    private final Predicate2<S, L> isEdgeClosed;

    private final Map.Transient<O, Collection<IResolutionPath<S, L, O>>> resolution;
    private final Map.Transient<S, Collection<O>> visibility;
    private final Map.Transient<S, Collection<O>> reachability;

    private final java.util.Map<O, IEsopEnv<S, L, O, IResolutionPath<S, L, O>>> pendingResolution;
    private final java.util.Map<S, IEsopEnv<S, L, O, IDeclPath<S, L, O>>> pendingVisibility;
    private final java.util.Map<S, IEsopEnv<S, L, O, IDeclPath<S, L, O>>> pendingReachability;
    private final java.util.Map<IRelation<L>, EnvL<S, L, O>> stagedEnv_L;

    public EsopNameResolution(IResolutionParameters<L> params, IEsopScopeGraph<S, L, O, ?> scopeGraph,
            Predicate2<S, L> isEdgeClosed, Map.Transient<O, Collection<IResolutionPath<S, L, O>>> resolution,
            Map.Transient<S, Collection<O>> visibility, Map.Transient<S, Collection<O>> reachability) {

        this.params = params;
        this.scopeGraph = scopeGraph;
        this.labels = CapsuleUtil.toSet(params.getLabels());
        this.labelD = params.getLabelD();
        this.labelR = params.getLabelR();
        this.wf = RegExpMatcher.create(params.getPathWf());
        this.order = params.getSpecificityOrder();
        assert order.getDescription().equals(
                RelationDescription.STRICT_PARTIAL_ORDER) : "Label specificity order must be a strict partial order";
        this.noOrder = Relation.Immutable.of(RelationDescription.STRICT_PARTIAL_ORDER);
        this.isEdgeClosed = isEdgeClosed;

        this.resolution = resolution;
        this.visibility = visibility;
        this.reachability = reachability;

        this.pendingResolution = new HashMap<>();
        this.pendingVisibility = new HashMap<>();
        this.pendingReachability = new HashMap<>();
        this.stagedEnv_L = new HashMap<>();
    }

    @Override public boolean addCached(IEsopNameResolution.IResolutionCache<S, L, O> cache) {
        if(!(cache instanceof ResolutionCache)) {
            return false;
        }
        final ResolutionCache<S, L, O> _cache = (ResolutionCache<S, L, O>) cache;
        boolean change = false;
        change |= resolution.__putAll(_cache.resolutionEntries());
        change |= visibility.__putAll(_cache.visibilityEntries());
        change |= reachability.__putAll(_cache.reachabilityEntries());
        return change;
    }

    @Override public ResolutionCache<S, L, O> toCache() {
        return new ResolutionCache<>(resolution.freeze(), visibility.freeze(), reachability.freeze());
    }

    @Override public Set<O> getResolvedRefs() {
        return Collections.unmodifiableSet(resolution.keySet());
    }

    @Override public Collection<IResolutionPath<S, L, O>> resolve(O ref, ICancel cancel, IProgress progress)
            throws CriticalEdgeException, InterruptedException {
        if(resolution.containsKey(ref)) {
            return resolution.get(ref);
        } else {
            final IEsopEnv<S, L, O, IResolutionPath<S, L, O>> env = pendingResolution.computeIfAbsent(ref,
                    r -> resolveEnv(io.usethesource.capsule.Set.Immutable.of(), ref));
            Collection<IResolutionPath<S, L, O>> result = env.get(cancel);
            resolution.__put(ref, result);
            pendingResolution.remove(ref);
            return result;
        }
    }

    @Override public Set<Map.Entry<O, Collection<IResolutionPath<S, L, O>>>> resolutionEntries() {
        return Collections.unmodifiableSet(resolution.entrySet());
    }

    @Override public Collection<O> decls(S scope) throws CriticalEdgeException {
        if(!isEdgeClosed.test(scope, labelD)) {
            throw new CriticalEdgeException(scope, labelD);
        } else {
            return scopeGraph.getDecls().inverse().get(scope);
        }
    }

    @Override public Collection<O> refs(S scope) throws CriticalEdgeException {
        if(!isEdgeClosed.test(scope, labelR)) {
            throw new CriticalEdgeException(scope, labelR);
        } else {
            return scopeGraph.getRefs().inverse().get(scope);
        }
    }

    @Override public Collection<O> visible(S scope, ICancel cancel, IProgress progress)
            throws CriticalEdgeException, InterruptedException {
        if(visibility.containsKey(scope)) {
            return visibility.get(scope);
        } else {
            final IEsopEnv<S, L, O, IDeclPath<S, L, O>> env =
                    pendingVisibility.computeIfAbsent(scope, s -> visibleEnv(scope));
            Collection<IDeclPath<S, L, O>> result = env.get(cancel);
            CapsuleUtil.SetBuilder<O> declsBuilder = new CapsuleUtil.SetBuilder<>();
            result.stream().map(IDeclPath::getDeclaration).forEach(declsBuilder::add);
            final Collection<O> decls = declsBuilder.build();
            visibility.__put(scope, decls);
            pendingVisibility.remove(scope);
            return decls;
        }
    }

    @Override public Collection<O> reachable(S scope, ICancel cancel, IProgress progress)
            throws CriticalEdgeException, InterruptedException {
        if(reachability.containsKey(scope)) {
            return reachability.get(scope);
        } else {
            final IEsopEnv<S, L, O, IDeclPath<S, L, O>> env =
                    pendingReachability.computeIfAbsent(scope, s -> reachableEnv(scope));
            Collection<IDeclPath<S, L, O>> result = env.get(cancel);
            CapsuleUtil.SetBuilder<O> declsBuilder = new CapsuleUtil.SetBuilder<>();
            result.stream().map(IDeclPath::getDeclaration).forEach(declsBuilder::add);
            final Collection<O> decls = declsBuilder.build();
            reachability.__put(scope, decls);
            pendingReachability.remove(scope);
            return decls;
        }
    }

    private IEsopEnv<S, L, O, IDeclPath<S, L, O>> visibleEnv(S scope) {
        return env(io.usethesource.capsule.Set.Immutable.of(), order, wf, Paths.empty(scope), EsopEnvs.envFilter());
    }

    private IEsopEnv<S, L, O, IDeclPath<S, L, O>> reachableEnv(S scope) {
        return env(io.usethesource.capsule.Set.Immutable.of(), noOrder, wf, Paths.empty(scope), EsopEnvs.envFilter());
    }

    private IEsopEnv<S, L, O, IResolutionPath<S, L, O>> resolveEnv(io.usethesource.capsule.Set.Immutable<O> seenI,
            O ref) {
        return scopeGraph.getRefs().get(ref)
                .map(scope -> env(seenI.__insert(ref), order, wf, Paths.empty(scope), EsopEnvs.resolutionFilter(ref)))
                .orElseGet(() -> EsopEnvs.empty());
    }

    private <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> env(io.usethesource.capsule.Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter) {
        if(re.isEmpty()) {
            return EsopEnvs.empty();
        } else {
            return env_L(labels, seenImports, lt, re, path, filter);
        }
    }

    @SuppressWarnings("unchecked") private <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> env_l(
            io.usethesource.capsule.Set.Immutable<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re, L l,
            IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter, ICancel cancel) {
        return EsopEnvs.guarded((EnvProvider<S, L, O, P> & Serializable) () -> {
            final S s = path.getTarget();
            if(scopeGraph.isOpen(s, l) || !isEdgeClosed.test(s, l)) {
                throw new CriticalEdgeException(s, l);
            } else {
                final IEsopEnv<S, L, O, P> env = l.equals(labelD) ? env_D(re, path, filter)
                        : env_nonD(seenImports, lt, re, l, path, filter, cancel);
                return env;
            }
        });
    }

    private <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> env_D(IRegExpMatcher<L> re, IScopePath<S, L, O> path,
            IEsopEnv.Filter<S, L, O, P> filter) {
        if(!re.isAccepting()) {
            return EsopEnvs.empty();
        } else {
            List<P> paths = new ArrayList<>();
            for(O decl : scopeGraph.getDecls().inverse().get(path.getTarget())) {
                final IDeclPath<S, L, O> p = params.getPathRelevance() ? Paths.decl(path, decl)
                        : Paths.decl(Paths.empty(path.getTarget()), decl);
                filter.test(p).ifPresent(paths::add);
            }
            return EsopEnvs.init(paths);
        }
    }

    private <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> env_nonD(
            io.usethesource.capsule.Set.Immutable<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re, L l,
            IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter, ICancel cancel) {
        @SuppressWarnings("unchecked") Function1<IScopePath<S, L, O>, IEsopEnv<S, L, O, P>> getter =
                (Function1<IScopePath<S, L, O>, IEsopEnv<S, L, O, P>> & Serializable) p -> {
                    return env(seenImports, lt, re.match(l), p, filter);
                };
        final List<IEsopEnv<S, L, O, P>> envs = new ArrayList<>();
        directScopes(l, path, getter, envs);
        importScopes(seenImports, l, path, getter, cancel,
            envs);
        return EsopEnvs.union(envs);
    }

    private <P extends IPath<S, L, O>> void directScopes(L l,
        IScopePath<S, L, O> path, Function1<IScopePath<S, L, O>, IEsopEnv<S, L, O, P>> getter,
        List<IEsopEnv<S, L, O, P>> envs) {
        for(S nextScope : scopeGraph.getDirectEdges().get(path.getTarget(), l)) {
            Paths.append(path, Paths.direct(path.getTarget(), l, nextScope)).map(getter::apply).ifPresent(
                envs::add);
        }
    }

    @SuppressWarnings("unchecked") private <P extends IPath<S, L, O>> void importScopes(
        io.usethesource.capsule.Set.Immutable<O> seenImports, L l, IScopePath<S, L, O> path,
        Function1<IScopePath<S, L, O>, IEsopEnv<S, L, O, P>> getter, ICancel cancel,
        List<IEsopEnv<S, L, O, P>> envs) {
        for(O ref : scopeGraph.getImportEdges().get(path.getTarget(), l)) {
            if(seenImports.contains(ref)) {
                continue;
            }
            final IEsopEnv<S, L, O, IResolutionPath<S, L, O>> env = resolveEnv(seenImports, ref);
            envs.add(EsopEnvs.guarded((EnvProvider<S, L, O, P> & Serializable) () -> {
                Collection<IResolutionPath<S, L, O>> paths = env.get(cancel);
                List<IEsopEnv<S, L, O, P>> importEnvs = new ArrayList<>();
                for(IResolutionPath<S, L, O> importPath : paths) {
                    for(S nextScope : scopeGraph.getExportEdges().get(importPath.getDeclaration(), l)) {
                        Paths.append(path, Paths.named(path.getTarget(), l, importPath, nextScope)).map(getter::apply)
                                .ifPresent(importEnvs::add);
                    }
                }
                return EsopEnvs.union(importEnvs);
            }));
        }
    }

    private <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> env_L(Set<L> L,
            io.usethesource.capsule.Set.Immutable<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re,
            IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter) {
        return stagedEnv_L.computeIfAbsent(lt, lo -> stageEnv_L(L, lt)).apply(seenImports, re, path, filter,
            new HashMap<L, IEsopEnv<S, L, O, P>>());
    }

    private EnvL<S, L, O> stageEnv_L(Set<L> L, IRelation<L> lt) {
        List<EnvL<S, L, O>> stagedEnvs = new ArrayList<>();
        for(L l : max(lt, L)) {
            EnvL<S, L, O> smallerEnv = stageEnv_L(smaller(lt, L, l), lt);
            stagedEnvs.add(new EnvL<S, L, O>() {

                @Override public <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> apply(
                        io.usethesource.capsule.Set.Immutable<O> seenI, IRegExpMatcher<L> re, IScopePath<S, L, O> path,
                        IEsopEnv.Filter<S, L, O, P> filter, java.util.Map<L, IEsopEnv<S, L, O, P>> env_lCache) {
                    @SuppressWarnings("unchecked") final IEsopEnv<S, L, O, P> env_l =
                            EsopEnvs.lazy((LazyEnv<S, L, O, P> & Serializable) (cancel) -> {
                                return env_lCache.computeIfAbsent(l,
                                        ll -> env_l(seenI, lt, re, l, path, filter, cancel));
                            });
                    return EsopEnvs.shadow(filter, smallerEnv.apply(seenI, re, path, filter, env_lCache), env_l);
                }

            });
        }
        return new EnvL<S, L, O>() {

            @Override public <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> apply(
                    io.usethesource.capsule.Set.Immutable<O> seenI, IRegExpMatcher<L> re, IScopePath<S, L, O> path,
                    IEsopEnv.Filter<S, L, O, P> filter, java.util.Map<L, IEsopEnv<S, L, O, P>> env_lCache) {
                return EsopEnvs.union(stagedEnvs.stream().map(se -> se.apply(seenI, re, path, filter, env_lCache))
                        .collect(Collectors.toList()));

            }

        };

    }

    private Set<L> max(IRelation<L> lt, Set<L> L) {
        CapsuleUtil.SetBuilder<L> maxL = new CapsuleUtil.SetBuilder<>();
        tryNext: for(L l : L) {
            for(L larger : lt.larger(l)) {
                if(L.contains(larger)) {
                    continue tryNext;
                }
            }
            maxL.add(l);
        }
        return maxL.build();
    }

    private Set<L> smaller(IRelation<L> lt, Set<L> L, L l) {
        CapsuleUtil.SetBuilder<L> smallerL = new CapsuleUtil.SetBuilder<>();
        for(L smaller : lt.smaller(l)) {
            if(L.contains(smaller)) {
                smallerL.add(smaller);
            }
        }
        return smallerL.build();
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> EsopNameResolution<S, L, O>
            of(IResolutionParameters<L> params, IEsopScopeGraph<S, L, O, ?> scopeGraph, Predicate2<S, L> isEdgeClosed) {
        return new EsopNameResolution<>(params, scopeGraph, isEdgeClosed, Map.Transient.of(), Map.Transient.of(),
                Map.Transient.of());
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> EsopNameResolution<S, L, O> of(
            IResolutionParameters<L> params, IEsopScopeGraph<S, L, O, ?> scopeGraph, Predicate2<S, L> isEdgeClosed,
            IEsopNameResolution.IResolutionCache<S, L, O> cache) {
        if(cache instanceof ResolutionCache) {
            final ResolutionCache<S, L, O> _cache = (ResolutionCache<S, L, O>) cache;
            return new EsopNameResolution<>(params, scopeGraph, isEdgeClosed, _cache.resolutionEntries().asTransient(),
                    _cache.visibilityEntries().asTransient(), _cache.reachabilityEntries().asTransient());
        } else {
            return new EsopNameResolution<>(params, scopeGraph, isEdgeClosed, Map.Transient.of(), Map.Transient.of(),
                    Map.Transient.of());
        }

    }

    private static class ResolutionCache<S extends IScope, L extends ILabel, O extends IOccurrence>
            implements IEsopNameResolution.IResolutionCache<S, L, O>, Serializable {

        private static final long serialVersionUID = 42L;

        private final Map.Immutable<O, Collection<IResolutionPath<S, L, O>>> resolutionCache;
        private final Map.Immutable<S, Collection<O>> visibilityCache;
        private final Map.Immutable<S, Collection<O>> reachabilityCache;

        private ResolutionCache(Map.Immutable<O, Collection<IResolutionPath<S, L, O>>> resolutionCache,
                Map.Immutable<S, Collection<O>> visibilityCache, Map.Immutable<S, Collection<O>> reachabilityCache) {
            this.resolutionCache = resolutionCache;
            this.visibilityCache = visibilityCache;
            this.reachabilityCache = reachabilityCache;
        }

        public Map.Immutable<O, Collection<IResolutionPath<S, L, O>>> resolutionEntries() {
            return resolutionCache;
        }

        public Map.Immutable<S, Collection<O>> visibilityEntries() {
            return visibilityCache;
        }

        public Map.Immutable<S, Collection<O>> reachabilityEntries() {
            return reachabilityCache;
        }

    }

}