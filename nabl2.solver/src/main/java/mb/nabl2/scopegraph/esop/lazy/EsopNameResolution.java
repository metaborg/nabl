package mb.nabl2.scopegraph.esop.lazy;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.PartialFunction0;
import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.usethesource.capsule.Map;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.relations.IRelation;
import mb.nabl2.relations.RelationDescription;
import mb.nabl2.relations.impl.Relation;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IResolutionParameters;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.esop.IEsopNameResolution;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.path.IPath;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.path.IScopePath;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.util.Tuple2;

public class EsopNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution<S, L, O> {

    private final IEsopScopeGraph<S, L, O, ?> scopeGraph;
    private final Set<L> labels;
    private final L labelD;
    private final IRegExpMatcher<L> wf;
    private final IRelation.Immutable<L> order;
    private final IRelation<L> noOrder;
    private final Predicate2<S, L> isEdgeClosed;

    private final Map.Transient<O, Set<IResolutionPath<S, L, O>>> resolution;
    private final Map.Transient<S, Set<O>> visibility;
    private final Map.Transient<S, Set<O>> reachability;

    private final java.util.Map<O, IEsopEnv<S, L, O, IResolutionPath<S, L, O>>> pendingResolution;
    private final java.util.Map<S, IEsopEnv<S, L, O, IDeclPath<S, L, O>>> pendingVisibility;
    private final java.util.Map<S, IEsopEnv<S, L, O, IDeclPath<S, L, O>>> pendingReachability;
    private final java.util.Map<IRelation<L>, EnvL<S, L, O>> stagedEnv_L;

    public EsopNameResolution(IResolutionParameters<L> params, IEsopScopeGraph<S, L, O, ?> scopeGraph,
            Predicate2<S, L> isEdgeClosed, Map.Transient<O, Set<IResolutionPath<S, L, O>>> resolution,
            Map.Transient<S, Set<O>> visibility, Map.Transient<S, Set<O>> reachability) {

        this.scopeGraph = scopeGraph;
        this.labels = ImmutableSet.copyOf(params.getLabels());
        this.labelD = params.getLabelD();
        this.wf = RegExpMatcher.create(params.getPathWf());
        this.order = params.getSpecificityOrder();
        assert order.getDescription().equals(
                RelationDescription.STRICT_PARTIAL_ORDER) : "Label specificity order must be a strict partial order";
        this.noOrder = Relation.Immutable.of(RelationDescription.STRICT_PARTIAL_ORDER);
        this.isEdgeClosed = isEdgeClosed;

        this.resolution = resolution;
        this.visibility = visibility;
        this.reachability = reachability;

        this.pendingResolution = Maps.newHashMap();
        this.pendingVisibility = Maps.newHashMap();
        this.pendingReachability = Maps.newHashMap();
        this.stagedEnv_L = Maps.newHashMap();
    }

    @Override public boolean addAll(IEsopNameResolution.ResolutionCache<S, L, O> cache) {
        boolean change = false;
        change |= resolution.__putAll(cache.resolutionEntries());
        change |= visibility.__putAll(cache.visibilityEntries());
        change |= reachability.__putAll(cache.reachabilityEntries());
        return change;
    }

    @Override public ResolutionCache<S, L, O> toCache() {
        return new ResolutionCache<>(resolution.freeze(), visibility.freeze(), reachability.freeze());
    }

    @Override public Set<O> getResolvedRefs() {
        return Collections.unmodifiableSet(resolution.keySet());
    }

    @Override public void resolveAll() {
        scopeGraph.getAllRefs().forEach(this::resolve);
    }

    @Override public Optional<Set<IResolutionPath<S, L, O>>> resolve(O ref) {
        if(resolution.containsKey(ref)) {
            return Optional.of(resolution.get(ref));
        } else {
            final IEsopEnv<S, L, O, IResolutionPath<S, L, O>> env = pendingResolution.computeIfAbsent(ref,
                    r -> resolveEnv(io.usethesource.capsule.Set.Immutable.of(), ref));
            return env.get().map(Tuple2::_1).map(result -> {
                resolution.put(ref, result);
                pendingResolution.remove(ref);
                return result;
            });
        }
    }

    @Override public Set<Map.Entry<O, Set<IResolutionPath<S, L, O>>>> resolutionEntries() {
        return Collections.unmodifiableSet(resolution.entrySet());
    }

    @Override public Optional<Set<O>> visible(S scope) {
        if(visibility.containsKey(scope)) {
            return Optional.of(visibility.get(scope));
        } else {
            final IEsopEnv<S, L, O, IDeclPath<S, L, O>> env =
                    pendingVisibility.computeIfAbsent(scope, s -> visibleEnv(scope));
            return env.get().map(Tuple2::_1).map(result -> {
                ImmutableSet.Builder<O> declsBuilder = ImmutableSet.builder();
                result.stream().map(IDeclPath::getDeclaration).forEach(declsBuilder::add);
                final Set<O> decls = declsBuilder.build();
                visibility.put(scope, decls);
                pendingVisibility.remove(scope);
                return decls;
            });
        }
    }

    @Override public Optional<Set<O>> reachable(S scope) {
        if(reachability.containsKey(scope)) {
            return Optional.of(reachability.get(scope));
        } else {
            final IEsopEnv<S, L, O, IDeclPath<S, L, O>> env =
                    pendingReachability.computeIfAbsent(scope, s -> reachableEnv(scope));
            return env.get().map(Tuple2::_1).map(result -> {
                ImmutableSet.Builder<O> declsBuilder = ImmutableSet.builder();
                result.stream().map(IDeclPath::getDeclaration).forEach(declsBuilder::add);
                final Set<O> decls = declsBuilder.build();
                reachability.put(scope, decls);
                pendingReachability.remove(scope);
                return decls;
            });
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
            return EsopEnvs.trace(path.getTarget().getResource(), env_L(labels, seenImports, lt, re, path, filter));
        }
    }

    @SuppressWarnings("unchecked") private <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> env_l(
            io.usethesource.capsule.Set.Immutable<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re, L l,
            IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter) {
        return EsopEnvs.guarded((PartialFunction0<IEsopEnv<S, L, O, P>> & Serializable) () -> {
            final S s = path.getTarget();
            if(scopeGraph.isOpen(s, l) || !isEdgeClosed.test(s, l)) {
                return Optional.empty();
            } else {
                final IEsopEnv<S, L, O, P> env =
                        l.equals(labelD) ? env_D(re, path, filter) : env_nonD(seenImports, lt, re, l, path, filter);
                return Optional.of(env);
            }
        });
    }

    private <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> env_D(IRegExpMatcher<L> re, IScopePath<S, L, O> path,
            IEsopEnv.Filter<S, L, O, P> filter) {
        if(!re.isAccepting()) {
            return EsopEnvs.empty();
        } else {
            List<P> paths = Lists.newArrayList();
            for(O decl : scopeGraph.getDecls().inverse().get(path.getTarget())) {
                filter.test(Paths.decl(path, decl)).ifPresent(paths::add);
            }
            return EsopEnvs.init(paths);
        }
    }

    private <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> env_nonD(
            io.usethesource.capsule.Set.Immutable<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re, L l,
            IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter) {
        @SuppressWarnings("unchecked") Function1<IScopePath<S, L, O>, IEsopEnv<S, L, O, P>> getter =
                (Function1<IScopePath<S, L, O>, IEsopEnv<S, L, O, P>> & Serializable) p -> {
                    return env(seenImports, lt, re.match(l), p, filter);
                };
        return EsopEnvs
                .union(Iterables.concat(directScopes(l, path, getter), importScopes(seenImports, l, path, getter)));
    }

    private <P extends IPath<S, L, O>> Iterable<IEsopEnv<S, L, O, P>> directScopes(L l, IScopePath<S, L, O> path,
            Function1<IScopePath<S, L, O>, IEsopEnv<S, L, O, P>> getter) {
        List<IEsopEnv<S, L, O, P>> envs = Lists.newArrayList();
        for(S nextScope : scopeGraph.getDirectEdges().get(path.getTarget(), l)) {
            Paths.append(path, Paths.direct(path.getTarget(), l, nextScope)).map(getter::apply).ifPresent(envs::add);
        }
        return envs;
    }

    @SuppressWarnings("unchecked") private <P extends IPath<S, L, O>> Iterable<IEsopEnv<S, L, O, P>> importScopes(
            io.usethesource.capsule.Set.Immutable<O> seenImports, L l, IScopePath<S, L, O> path,
            Function1<IScopePath<S, L, O>, IEsopEnv<S, L, O, P>> getter) {
        List<IEsopEnv<S, L, O, P>> envs = Lists.newArrayList();
        for(O ref : scopeGraph.getImportEdges().get(path.getTarget(), l)) {
            if(seenImports.contains(ref)) {
                continue;
            }
            final IEsopEnv<S, L, O, IResolutionPath<S, L, O>> env = resolveEnv(seenImports, ref);
            envs.add(EsopEnvs.guarded((PartialFunction0<IEsopEnv<S, L, O, P>> & Serializable) () -> {
                return env.get().map(paths -> {
                    List<IEsopEnv<S, L, O, P>> importEnvs = Lists.newArrayList();
                    for(IResolutionPath<S, L, O> importPath : paths._1()) {
                        for(S nextScope : scopeGraph.getExportEdges().get(importPath.getDeclaration(), l)) {
                            Paths.append(path, Paths.named(path.getTarget(), l, importPath, nextScope))
                                    .map(getter::apply).ifPresent(importEnvs::add);
                        }
                    }
                    return EsopEnvs.trace(EsopEnvs.union(importEnvs), paths._2());
                });
            }));

        }
        return envs;
    }

    private <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> env_L(Set<L> L,
            io.usethesource.capsule.Set.Immutable<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re,
            IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter) {
        return stagedEnv_L.computeIfAbsent(lt, lo -> stageEnv_L(L, lt)).apply(seenImports, re, path, filter,
                Maps.newHashMap());
    }

    private EnvL<S, L, O> stageEnv_L(Set<L> L, IRelation<L> lt) {
        List<EnvL<S, L, O>> stagedEnvs = Lists.newArrayList();
        for(L l : max(lt, L)) {
            EnvL<S, L, O> smallerEnv = stageEnv_L(smaller(lt, L, l), lt);
            stagedEnvs.add(new EnvL<S, L, O>() {

                @Override public <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> apply(
                        io.usethesource.capsule.Set.Immutable<O> seenI, IRegExpMatcher<L> re, IScopePath<S, L, O> path,
                        IEsopEnv.Filter<S, L, O, P> filter, java.util.Map<L, IEsopEnv<S, L, O, P>> env_lCache) {
                    @SuppressWarnings("unchecked") final IEsopEnv<S, L, O, P> env_l =
                            EsopEnvs.lazy((Function0<IEsopEnv<S, L, O, P>> & Serializable) () -> {
                                return env_lCache.computeIfAbsent(l, ll -> env_l(seenI, lt, re, l, path, filter));
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
        ImmutableSet.Builder<L> maxL = ImmutableSet.builder();
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
        ImmutableSet.Builder<L> smallerL = ImmutableSet.builder();
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

    public static class ResolutionCache<S extends IScope, L extends ILabel, O extends IOccurrence>
            implements IEsopNameResolution.ResolutionCache<S, L, O>, Serializable {
        private static final long serialVersionUID = 42L;

        private final Map.Immutable<O, Set<IResolutionPath<S, L, O>>> resolutionCache;
        private final Map.Immutable<S, Set<O>> visibilityCache;
        private final Map.Immutable<S, Set<O>> reachabilityCache;

        private ResolutionCache(Map.Immutable<O, Set<IResolutionPath<S, L, O>>> resolutionCache,
                Map.Immutable<S, Set<O>> visibilityCache, Map.Immutable<S, Set<O>> reachabilityCache) {
            this.resolutionCache = resolutionCache;
            this.visibilityCache = visibilityCache;
            this.reachabilityCache = reachabilityCache;
        }

        @Override public Map.Immutable<O, Set<IResolutionPath<S, L, O>>> resolutionEntries() {
            return resolutionCache;
        }

        @Override public Map.Immutable<S, Set<O>> visibilityEntries() {
            return visibilityCache;
        }

        @Override public Map.Immutable<S, Set<O>> reachabilityEntries() {
            return reachabilityCache;
        }

        public static <S extends IScope, L extends ILabel, O extends IOccurrence> ResolutionCache<S, L, O> of() {
            return new ResolutionCache<>(Map.Immutable.of(), Map.Immutable.of(), Map.Immutable.of());
        }

    }

}