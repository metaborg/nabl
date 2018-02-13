package org.metaborg.meta.nabl2.scopegraph.esop.reference;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.regexp.RegExpMatcher;
import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.RelationDescription;
import org.metaborg.meta.nabl2.relations.terms.Relation;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.Tuple2;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.PartialFunction0;
import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;

public abstract class EsopNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution<S, L, O> {

    protected EsopNameResolution() {
    }

    public static class Immutable<S extends IScope, L extends ILabel, O extends IOccurrence>
            extends EsopNameResolution<S, L, O> implements IEsopNameResolution.Immutable<S, L, O>, Serializable {
        private static final long serialVersionUID = 42L;

        private final IResolutionParameters<L> params;

        private final Map.Immutable<O, Set.Immutable<IResolutionPath<S, L, O>>> resolution;

        private Immutable(IResolutionParameters<L> params,
                Map.Immutable<O, Set.Immutable<IResolutionPath<S, L, O>>> resolution) {
            this.params = params;
            this.resolution = resolution;
        }

        @Override public java.util.Set<O> getResolvedRefs() {
            return resolution.keySet();
        }

        @Override public Optional<Set.Immutable<IResolutionPath<S, L, O>>> resolve(O ref) {
            return Optional.ofNullable(resolution.get(ref));
        }

        @Override public java.util.Set<Entry<O, io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>>>
                resolutionEntries() {
            return resolution.entrySet();
        }

        @Override public Optional<Set.Immutable<O>> visible(S scope) {
            return Optional.empty();
        }

        @Override public Optional<Set.Immutable<O>> reachable(S scope) {
            return Optional.empty();
        }

        @Override public IEsopNameResolution.Transient<S, L, O> melt(IEsopScopeGraph<S, L, O, ?> scopeGraph,
                Predicate2<S, L> isEdgeClosed) {
            return new EsopNameResolution.Transient<>(params, scopeGraph, isEdgeClosed, resolution.asTransient(),
                    Map.Transient.of(), Map.Transient.of());
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + params.hashCode();
            result = prime * result + resolution.hashCode();
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") final EsopNameResolution.Immutable<S, L, O> other =
                    (EsopNameResolution.Immutable<S, L, O>) obj;
            if(!params.equals(other.params))
                return false;
            if(!resolution.equals(other.resolution))
                return false;
            return true;
        }

        public static <S extends IScope, L extends ILabel, O extends IOccurrence> EsopNameResolution.Immutable<S, L, O>
                of(IResolutionParameters<L> params) {
            return new EsopNameResolution.Immutable<>(params, Map.Immutable.of());
        }

    }

    public static class Transient<S extends IScope, L extends ILabel, O extends IOccurrence>
            extends EsopNameResolution<S, L, O> implements IEsopNameResolution.Transient<S, L, O> {

        private final IResolutionParameters<L> params;

        private final IEsopScopeGraph<S, L, O, ?> scopeGraph;
        private final Set<L> labels;
        private final L labelD;
        private final IRegExpMatcher<L> wf;
        private final IRelation.Immutable<L> order;
        private final IRelation<L> noOrder;
        private final Predicate2<S, L> isEdgeClosed;

        private final Map.Transient<O, Set.Immutable<IResolutionPath<S, L, O>>> resolution;
        private final Map.Transient<S, Set.Immutable<O>> visibility;
        private final Map.Transient<S, Set.Immutable<O>> reachability;

        private final java.util.Map<O, IEsopEnv<S, L, O, IResolutionPath<S, L, O>>> pendingResolution;
        private final java.util.Map<S, IEsopEnv<S, L, O, IDeclPath<S, L, O>>> pendingVisibility;
        private final java.util.Map<S, IEsopEnv<S, L, O, IDeclPath<S, L, O>>> pendingReachability;
        private final java.util.Map<IRelation<L>, EnvL<S, L, O>> stagedEnv_L;

        public Transient(IResolutionParameters<L> params, IEsopScopeGraph<S, L, O, ?> scopeGraph,
                Predicate2<S, L> isEdgeClosed, Map.Transient<O, Set.Immutable<IResolutionPath<S, L, O>>> resolution,
                Map.Transient<S, Set.Immutable<O>> visibility, Map.Transient<S, Set.Immutable<O>> reachability) {
            this.params = params;

            this.scopeGraph = scopeGraph;
            this.labels = Set.Immutable.<L>of().__insertAll(Sets.newHashSet(params.getLabels()));
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

        @Override public java.util.Set<O> getResolvedRefs() {
            return Collections.unmodifiableSet(resolution.keySet());
        }

        @Override public boolean addAll(IEsopNameResolution<S, L, O> other) {
            for(Map.Entry<O, Set.Immutable<IResolutionPath<S, L, O>>> entry : other.resolutionEntries()) {
                resolution.__put(entry.getKey(), entry.getValue());
            }
            return !other.resolutionEntries().isEmpty();
        }

        @Override public Optional<Set.Immutable<IResolutionPath<S, L, O>>> resolve(O ref) {
            if(resolution.containsKey(ref)) {
                return Optional.of(resolution.get(ref));
            } else {
                final IEsopEnv<S, L, O, IResolutionPath<S, L, O>> env =
                        pendingResolution.computeIfAbsent(ref, r -> resolveEnv(Set.Immutable.of(), ref));
                return env.get().map(Tuple2::_1).map(result -> {
                    resolution.__put(ref, result);
                    pendingResolution.remove(ref);
                    return result;
                });
            }
        }

        @Override public java.util.Set<Map.Entry<O, Set.Immutable<IResolutionPath<S, L, O>>>> resolutionEntries() {
            return resolution.entrySet();
        }

        @Override public Optional<Set.Immutable<O>> visible(S scope) {
            if(visibility.containsKey(scope)) {
                return Optional.of(visibility.get(scope));
            } else {
                final IEsopEnv<S, L, O, IDeclPath<S, L, O>> env =
                        pendingVisibility.computeIfAbsent(scope, s -> visibleEnv(scope));
                return env.get().map(Tuple2::_1).map(result -> {
                    Set.Transient<O> declsBuilder = Set.Transient.of();
                    result.stream().map(IDeclPath::getDeclaration).forEach(declsBuilder::__insert);
                    Set.Immutable<O> decls = declsBuilder.freeze();
                    visibility.__put(scope, decls);
                    pendingVisibility.remove(scope);
                    return decls;
                });
            }
        }

        @Override public Optional<Set.Immutable<O>> reachable(S scope) {
            if(reachability.containsKey(scope)) {
                return Optional.of(reachability.get(scope));
            } else {
                final IEsopEnv<S, L, O, IDeclPath<S, L, O>> env =
                        pendingReachability.computeIfAbsent(scope, s -> reachableEnv(scope));
                return env.get().map(Tuple2::_1).map(result -> {
                    Set.Transient<O> declsBuilder = Set.Transient.of();
                    result.stream().map(IDeclPath::getDeclaration).forEach(declsBuilder::__insert);
                    Set.Immutable<O> decls = declsBuilder.freeze();
                    reachability.__put(scope, decls);
                    pendingReachability.remove(scope);
                    return decls;
                });
            }
        }

        private IEsopEnv<S, L, O, IDeclPath<S, L, O>> visibleEnv(S scope) {
            return env(Set.Immutable.of(), order, wf, Paths.empty(scope), EsopEnvs.envFilter());
        }

        private IEsopEnv<S, L, O, IDeclPath<S, L, O>> reachableEnv(S scope) {
            return env(Set.Immutable.of(), noOrder, wf, Paths.empty(scope), EsopEnvs.envFilter());
        }

        private IEsopEnv<S, L, O, IResolutionPath<S, L, O>> resolveEnv(Set.Immutable<O> seenI, O ref) {
            return scopeGraph.getRefs().get(ref).map(
                    scope -> env(seenI.__insert(ref), order, wf, Paths.empty(scope), EsopEnvs.resolutionFilter(ref)))
                    .orElseGet(() -> EsopEnvs.empty());
        }

        private <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> env(Set.Immutable<O> seenImports, IRelation<L> lt,
                IRegExpMatcher<L> re, IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter) {
            if(re.isEmpty()) {
                return EsopEnvs.empty();
            } else {
                return EsopEnvs.trace(path.getTarget().getResource(), env_L(labels, seenImports, lt, re, path, filter));
            }
        }

        private <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> env_l(Set.Immutable<O> seenImports, IRelation<L> lt,
                IRegExpMatcher<L> re, L l, IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter) {
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

        private <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> env_nonD(Set.Immutable<O> seenImports, IRelation<L> lt,
                IRegExpMatcher<L> re, L l, IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter) {
            Function1<IScopePath<S, L, O>, IEsopEnv<S, L, O, P>> getter =
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
                Paths.append(path, Paths.direct(path.getTarget(), l, nextScope)).map(getter::apply)
                        .ifPresent(envs::add);
            }
            return envs;
        }

        private <P extends IPath<S, L, O>> Iterable<IEsopEnv<S, L, O, P>> importScopes(Set.Immutable<O> seenImports,
                L l, IScopePath<S, L, O> path, Function1<IScopePath<S, L, O>, IEsopEnv<S, L, O, P>> getter) {
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

        private <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> env_L(Set<L> L, Set.Immutable<O> seenImports,
                IRelation<L> lt, IRegExpMatcher<L> re, IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter) {
            return stagedEnv_L.computeIfAbsent(lt, lo -> stageEnv_L(L, lt)).apply(seenImports, re, path, filter,
                    Maps.newHashMap());
        }

        private EnvL<S, L, O> stageEnv_L(Set<L> L, IRelation<L> lt) {
            List<EnvL<S, L, O>> stagedEnvs = Lists.newArrayList();
            for(L l : max(lt, L)) {
                EnvL<S, L, O> smallerEnv = stageEnv_L(smaller(lt, L, l), lt);
                stagedEnvs.add(new EnvL<S, L, O>() {

                    @Override public <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> apply(Set.Immutable<O> seenI,
                            IRegExpMatcher<L> re, IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter,
                            java.util.Map<L, IEsopEnv<S, L, O, P>> env_lCache) {
                        final IEsopEnv<S, L, O, P> env_l =
                                EsopEnvs.lazy((Function0<IEsopEnv<S, L, O, P>> & Serializable) () -> {
                                    return env_lCache.computeIfAbsent(l, ll -> env_l(seenI, lt, re, l, path, filter));
                                });
                        return EsopEnvs.shadow(filter, smallerEnv.apply(seenI, re, path, filter, env_lCache), env_l);
                    }

                });
            }
            return new EnvL<S, L, O>() {

                @Override public <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> apply(Set.Immutable<O> seenI,
                        IRegExpMatcher<L> re, IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter,
                        java.util.Map<L, IEsopEnv<S, L, O, P>> env_lCache) {
                    return EsopEnvs.union(stagedEnvs.stream().map(se -> se.apply(seenI, re, path, filter, env_lCache))
                            .collect(Collectors.toList()));

                }

            };

        }

        private Set.Immutable<L> max(IRelation<L> lt, Set<L> L) {
            Set.Transient<L> maxL = Set.Transient.of();
            tryNext: for(L l : L) {
                for(L larger : lt.larger(l)) {
                    if(L.contains(larger)) {
                        continue tryNext;
                    }
                }
                maxL.__insert(l);
            }
            return maxL.freeze();
        }

        private Set.Immutable<L> smaller(IRelation<L> lt, Set<L> L, L l) {
            Set.Transient<L> smallerL = Set.Transient.of();
            for(L smaller : lt.smaller(l)) {
                if(L.contains(smaller)) {
                    smallerL.__insert(smaller);
                }
            }
            return smallerL.freeze();
        }

        @Override public IEsopNameResolution.Immutable<S, L, O> freeze() {
            return new EsopNameResolution.Immutable<>(params, resolution.freeze());
        }

        public static <S extends IScope, L extends ILabel, O extends IOccurrence> EsopNameResolution.Transient<S, L, O>
                of(IResolutionParameters<L> params, IEsopScopeGraph<S, L, O, ?> scopeGraph,
                        Predicate2<S, L> isEdgeClosed) {
            return new EsopNameResolution.Transient<>(params, scopeGraph, isEdgeClosed, Map.Transient.of(),
                    Map.Transient.of(), Map.Transient.of());
        }

    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IEsopNameResolution.Transient<S, L, O>
            extend(IEsopNameResolution.Transient<S, L, O> resolution1, IEsopNameResolution<S, L, O> resolution2) {
        return new Extension<>(resolution1, resolution2);
    }

    private static class Extension<S extends IScope, L extends ILabel, O extends IOccurrence>
            implements IEsopNameResolution.Transient<S, L, O> {

        private final IEsopNameResolution.Transient<S, L, O> resolution1;
        private final IEsopNameResolution<S, L, O> resolution2;

        protected Extension(org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution.Transient<S, L, O> resolution1,
                IEsopNameResolution<S, L, O> resolution2) {
            this.resolution1 = resolution1;
            this.resolution2 = resolution2;
        }

        @Override public java.util.Set<O> getResolvedRefs() {
            return Sets.union(resolution1.getResolvedRefs(), resolution2.getResolvedRefs());
        }

        @Override public Optional<Set.Immutable<IResolutionPath<S, L, O>>> resolve(O ref) {
            return resolution2.resolve(ref).map(Optional::of).orElseGet(() -> resolution1.resolve(ref));
        }

        @Override public java.util.Set<Entry<O, io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>>>
                resolutionEntries() {
            return Sets.union(resolution1.resolutionEntries(), resolution2.resolutionEntries());
        }

        @Override public Optional<Set.Immutable<O>> visible(S scope) {
            return resolution2.visible(scope).map(Optional::of).orElseGet(() -> resolution1.visible(scope));
        }

        @Override public Optional<Set.Immutable<O>> reachable(S scope) {
            return resolution2.reachable(scope).map(Optional::of).orElseGet(() -> resolution1.reachable(scope));
        }

        @Override public boolean addAll(IEsopNameResolution<S, L, O> other) {
            return resolution1.addAll(other);
        }

        @Override public IEsopNameResolution.Immutable<S, L, O> freeze() {
            return resolution1.freeze();
        }

    }

}
