package mb.nabl2.scopegraph.esop.reference;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

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

public class EsopNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution<S, L, O> {

    private final IEsopScopeGraph<S, L, O, ?> scopeGraph;
    private final Set<L> labels;
    private final L labelD;
    private final L labelR;
    private final IRegExpMatcher<L> wf;
    private final IRelation.Immutable<L> order;
    private final IRelation<L> noOrder;
    private final Predicate2<S, L> isEdgeClosed;

    private final Map.Transient<O, Set<IResolutionPath<S, L, O>>> resolution;
    private final Map.Transient<S, Set<O>> visibility;
    private final Map.Transient<S, Set<O>> reachability;

    public EsopNameResolution(IResolutionParameters<L> params, IEsopScopeGraph<S, L, O, ?> scopeGraph,
            Predicate2<S, L> isEdgeClosed, Map.Transient<O, Set<IResolutionPath<S, L, O>>> resolution,
            Map.Transient<S, Set<O>> visibility, Map.Transient<S, Set<O>> reachability) {

        this.scopeGraph = scopeGraph;
        this.labels = ImmutableSet.copyOf(params.getLabels());
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
            final Optional<Set<IResolutionPath<S, L, O>>> env =
                    resolveEnv(io.usethesource.capsule.Set.Immutable.of(), ref);
            return env.map(result -> {
                resolution.put(ref, result);
                return result;
            });
        }
    }

    @Override public Set<Map.Entry<O, Set<IResolutionPath<S, L, O>>>> resolutionEntries() {
        return Collections.unmodifiableSet(resolution.entrySet());
    }

    @Override public Optional<Set<O>> decls(S scope) {
        if(!isEdgeClosed.test(scope, labelD)) {
            return Optional.empty();
        } else {
            return Optional.of(scopeGraph.getDecls().inverse().get(scope));
        }
    }

    @Override public Optional<Set<O>> refs(S scope) {
        if(!isEdgeClosed.test(scope, labelR)) {
            return Optional.empty();
        } else {
            return Optional.of(scopeGraph.getRefs().inverse().get(scope));
        }
    }

    @Override public Optional<Set<O>> visible(S scope) {
        if(visibility.containsKey(scope)) {
            return Optional.of(visibility.get(scope));
        } else {
            final Optional<Set<IDeclPath<S, L, O>>> env = visibleEnv(scope);
            return env.map(result -> {
                ImmutableSet.Builder<O> declsBuilder = ImmutableSet.builder();
                result.stream().map(IDeclPath::getDeclaration).forEach(declsBuilder::add);
                final Set<O> decls = declsBuilder.build();
                visibility.put(scope, decls);
                return decls;
            });
        }
    }

    @Override public Optional<Set<O>> reachable(S scope) {
        if(reachability.containsKey(scope)) {
            return Optional.of(reachability.get(scope));
        } else {
            final Optional<Set<IDeclPath<S, L, O>>> env = reachableEnv(scope);
            return env.map(result -> {
                ImmutableSet.Builder<O> declsBuilder = ImmutableSet.builder();
                result.stream().map(IDeclPath::getDeclaration).forEach(declsBuilder::add);
                final Set<O> decls = declsBuilder.build();
                reachability.put(scope, decls);
                return decls;
            });
        }
    }

    private Optional<Set<IDeclPath<S, L, O>>> visibleEnv(S scope) {
        return env(io.usethesource.capsule.Set.Immutable.of(), order, wf, Paths.empty(scope), EsopFilters.envFilter());
    }

    private Optional<Set<IDeclPath<S, L, O>>> reachableEnv(S scope) {
        return env(io.usethesource.capsule.Set.Immutable.of(), noOrder, wf, Paths.empty(scope),
                EsopFilters.envFilter());
    }

    private Optional<Set<IResolutionPath<S, L, O>>> resolveEnv(io.usethesource.capsule.Set.Immutable<O> seenI, O ref) {
        final Optional<S> scope = scopeGraph.getRefs().get(ref);
        if(scope.isPresent()) {
            return env(seenI.__insert(ref), order, wf, Paths.empty(scope.get()), EsopFilters.resolutionFilter(ref));
        } else {
            return Optional.of(ImmutableSet.of());
        }
    }

    private <P extends IPath<S, L, O>> Optional<Set<P>> env(io.usethesource.capsule.Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, IScopePath<S, L, O> path, EsopFilters.Filter<S, L, O, P> filter) {
        if(re.isEmpty()) {
            return Optional.of(ImmutableSet.of());
        } else {
            return env_L(labels, seenImports, lt, re, path, filter);
        }
    }

    private <P extends IPath<S, L, O>> Optional<Set<P>> env_l(io.usethesource.capsule.Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, L l, IScopePath<S, L, O> path,
            EsopFilters.Filter<S, L, O, P> filter) {
        final S s = path.getTarget();
        if(scopeGraph.isOpen(s, l) || !isEdgeClosed.test(s, l)) {
            return Optional.empty();
        } else {
            final Optional<Set<P>> env =
                    l.equals(labelD) ? env_D(re, path, filter) : env_nonD(seenImports, lt, re, l, path, filter);
            return env;
        }
    }

    private <P extends IPath<S, L, O>> Optional<Set<P>> env_D(IRegExpMatcher<L> re, IScopePath<S, L, O> path,
            EsopFilters.Filter<S, L, O, P> filter) {
        ImmutableSet.Builder<P> env = ImmutableSet.builder();
        if(re.isAccepting()) {
            for(O decl : scopeGraph.getDecls().inverse().get(path.getTarget())) {
                filter.test(Paths.decl(path, decl)).ifPresent(env::add);
            }
        }
        return Optional.of(env.build());
    }

    private <P extends IPath<S, L, O>> Optional<Set<P>> env_nonD(io.usethesource.capsule.Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, L l, IScopePath<S, L, O> path,
            EsopFilters.Filter<S, L, O, P> filter) {
        return Optionals.lift(directScopes(l, path), importScopes(seenImports, l, path), (ps1, ps2) -> {
            final ImmutableSet.Builder<P> unionEnv = ImmutableSet.builder();
            for(IScopePath<S, L, O> p : Iterables.concat(ps1, ps2)) {
                Optional<Set<P>> env = env(seenImports, lt, re.match(l), p, filter);
                if(env.isPresent()) {
                    unionEnv.addAll(env.get());
                } else {
                    return Optional.<Set<P>>empty();
                }
            }
            return Optional.<Set<P>>of(unionEnv.build());
        }).flatMap(o -> o);
    }

    private Optional<List<IScopePath<S, L, O>>> directScopes(L l, IScopePath<S, L, O> path) {
        final List<IScopePath<S, L, O>> paths = Lists.newArrayList();
        for(S nextScope : scopeGraph.getDirectEdges().get(path.getTarget(), l)) {
            Paths.append(path, Paths.direct(path.getTarget(), l, nextScope)).ifPresent(paths::add);
        }
        return Optional.of(paths);
    }

    private Optional<List<IScopePath<S, L, O>>> importScopes(io.usethesource.capsule.Set.Immutable<O> seenImports, L l,
            IScopePath<S, L, O> path) {
        final List<IScopePath<S, L, O>> paths = Lists.newArrayList();
        for(O ref : scopeGraph.getImportEdges().get(path.getTarget(), l)) {
            if(seenImports.contains(ref)) {
                continue;
            }
            final Optional<Set<IResolutionPath<S, L, O>>> env = resolveEnv(seenImports, ref);
            if(env.isPresent()) {
                for(IResolutionPath<S, L, O> importPath : env.get()) {
                    for(S nextScope : scopeGraph.getExportEdges().get(importPath.getDeclaration(), l)) {
                        Paths.append(path, Paths.named(path.getTarget(), l, importPath, nextScope))
                                .ifPresent(paths::add);
                    }
                }
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(paths);
    }

    private <P extends IPath<S, L, O>> Optional<Set<P>> env_L(Set<L> L,
            io.usethesource.capsule.Set.Immutable<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re,
            IScopePath<S, L, O> path, EsopFilters.Filter<S, L, O, P> filter) {
        final Set<L> min_L = min(lt, L);
        final ImmutableSet.Builder<P> envBuilder = ImmutableSet.builder();
        for(L l : min_L) {
            final Optional<Set<P>> env_l = env_l(seenImports, lt, re, l, path, filter);
            if(env_l.isPresent()) {
                envBuilder.addAll(env_l.get());
            } else {
                return Optional.empty();
            }
        }
        final Set<P> env = envBuilder.build();
        if(filter.shortCircuit() && !env.isEmpty()) {
            return Optional.of(env);
        } else {
            final Set<Object> shadowTokens = env.stream().map(p -> filter.matchToken(p)).collect(Collectors.toSet());
            final ImmutableSet.Builder<P> deepEnvBuilder = ImmutableSet.builder();
            deepEnvBuilder.addAll(env);
            for(L l : min_L) {
                final Optional<Set<P>> env_L = env_L(larger(lt, L, l), seenImports, lt, re, path, filter);
                if(env_L.isPresent()) {
                    env_L.get().stream().filter(p -> !shadowTokens.contains(filter.matchToken(p)))
                            .forEach(deepEnvBuilder::add);
                } else {
                    return Optional.empty();
                }
            }
            return Optional.of(deepEnvBuilder.build());
        }
    }

    private Set<L> min(IRelation<L> lt, Set<L> L) {
        ImmutableSet.Builder<L> minL = ImmutableSet.builder();
        tryNext: for(L l1 : L) {
            for(L l2 : L) {
                if(lt.contains(l2, l1)) {
                    continue tryNext;
                }
            }
            minL.add(l1);
        }
        return minL.build();
    }

    private Set<L> larger(IRelation<L> lt, Set<L> L, L l1) {
        ImmutableSet.Builder<L> largerL = ImmutableSet.builder();
        for(L l2 : L) {
            if(lt.contains(l1, l2)) {
                largerL.add(l2);
            }
        }
        return largerL.build();
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