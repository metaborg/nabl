package org.metaborg.meta.nabl2.scopegraph.esop.reference;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
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
import org.metaborg.meta.nabl2.util.functions.Function0;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.meta.nabl2.util.functions.PartialFunction0;
import org.metaborg.meta.nabl2.util.functions.Predicate2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;

public class EsopNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution<S, L, O>, Serializable {

    private static final long serialVersionUID = 42L;


    private final IEsopScopeGraph<S, L, O, ?> scopeGraph;
    private final Set<L> labels;
    private final L labelD;
    private final IRegExpMatcher<L> wf;
    private final IRelation.Immutable<L> order;
    private final IRelation<L> noOrder;
    private final Predicate2<S, L> isEdgeClosed;

    transient private Map<O, IEsopEnv<S, L, O, IResolutionPath<S, L, O>>> resolveCache;
    transient private Map<S, IEsopEnv<S, L, O, IDeclPath<S, L, O>>> visibleCache;
    transient private Map<S, IEsopEnv<S, L, O, IDeclPath<S, L, O>>> reachableCache;
    transient private Map<IRelation<L>, EnvL<S, L, O>> stagedEnv_L;

    public EsopNameResolution(IEsopScopeGraph<S, L, O, ?> scopeGraph, IResolutionParameters<L> params,
            Predicate2<S, L> isEdgeClosed) {
        this.scopeGraph = scopeGraph;
        this.labels = Set.Immutable.<L>of().__insertAll(Sets.newHashSet(params.getLabels()));
        this.labelD = params.getLabelD();
        this.wf = RegExpMatcher.create(params.getPathWf());
        this.order = params.getSpecificityOrder();
        assert order.getDescription().equals(
                RelationDescription.STRICT_PARTIAL_ORDER) : "Label specificity order must be a strict partial order";
        this.noOrder = Relation.Immutable.of(RelationDescription.STRICT_PARTIAL_ORDER);
        this.isEdgeClosed = isEdgeClosed;
        initTransients();
    }

    private void initTransients() {
        this.resolveCache = Maps.newHashMap();
        this.visibleCache = Maps.newHashMap();
        this.reachableCache = Maps.newHashMap();
        this.stagedEnv_L = Maps.newHashMap();
    }

    @Override public Set<S> getAllScopes() {
        return scopeGraph.getAllScopes();
    }

    @Override public Set<O> getAllRefs() {
        return scopeGraph.getAllRefs();
    }

    @Override public Set.Immutable<IResolutionPath<S, L, O>> resolve(O ref) {
        return tryResolve(ref).map(Tuple2::_1).orElse(Set.Immutable.of());
    }

    @Override public Set.Immutable<IDeclPath<S, L, O>> visible(S scope) {
        return tryVisible(scope).map(Tuple2::_1).orElse(Set.Immutable.of());
    }

    @Override public Set.Immutable<IDeclPath<S, L, O>> reachable(S scope) {
        return tryReachable(scope).map(Tuple2::_1).orElse(Set.Immutable.of());
    }

    @Override public Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>>
            tryResolve(O ref) {
        final IEsopEnv<S, L, O, IResolutionPath<S, L, O>> env =
                resolveCache.computeIfAbsent(ref, r -> resolveEnv(Set.Immutable.of(), ref));
        return env.get();
    }

    @Override public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryVisible(S scope) {
        final IEsopEnv<S, L, O, IDeclPath<S, L, O>> env = visibleCache.computeIfAbsent(scope, s -> visibleEnv(scope));
        return env.get();
    }

    @Override public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryReachable(S scope) {
        final IEsopEnv<S, L, O, IDeclPath<S, L, O>> env =
                reachableCache.computeIfAbsent(scope, s -> reachableEnv(scope));
        return env.get();
    }

    private IEsopEnv<S, L, O, IDeclPath<S, L, O>> visibleEnv(S scope) {
        return env(Set.Immutable.of(), order, wf, Paths.empty(scope), EsopEnvs.envFilter());
    }

    private IEsopEnv<S, L, O, IDeclPath<S, L, O>> reachableEnv(S scope) {
        return env(Set.Immutable.of(), noOrder, wf, Paths.empty(scope), EsopEnvs.envFilter());
    }

    private IEsopEnv<S, L, O, IResolutionPath<S, L, O>> resolveEnv(Set.Immutable<O> seenI, O ref) {
        return scopeGraph.getRefs().get(ref)
                .map(scope -> env(seenI.__insert(ref), order, wf, Paths.empty(scope), EsopEnvs.resolutionFilter(ref)))
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
            if(!isEdgeClosed.test(path.getTarget(), l)) {
                return Optional.empty();
            } else {
                final IEsopEnv<S, L, O, P> env = l.equals(labelD) ? env_D(seenImports, lt, re, path, filter)
                        : env_nonD(seenImports, lt, re, l, path, filter);
                return Optional.of(env);
            }
        });
    }

    private <P extends IPath<S, L, O>> IEsopEnv<S, L, O, P> env_D(Set.Immutable<O> seenImports, IRelation<L> lt,
            IRegExpMatcher<L> re, IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter) {
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
        return EsopEnvs.union(Iterables.concat(directScopes(l, path, filter, getter),
                importScopes(seenImports, l, path, filter, getter)));
    }

    private <P extends IPath<S, L, O>> Iterable<IEsopEnv<S, L, O, P>> directScopes(L l, IScopePath<S, L, O> path,
            IEsopEnv.Filter<S, L, O, P> filter, Function1<IScopePath<S, L, O>, IEsopEnv<S, L, O, P>> getter) {
        List<IEsopEnv<S, L, O, P>> envs = Lists.newArrayList();
        for(S nextScope : scopeGraph.getDirectEdges().get(path.getTarget(), l)) {
            Paths.append(path, Paths.direct(path.getTarget(), l, nextScope)).map(getter::apply).ifPresent(envs::add);
        }
        return envs;
    }

    private <P extends IPath<S, L, O>> Iterable<IEsopEnv<S, L, O, P>> importScopes(Set.Immutable<O> seenImports, L l,
            IScopePath<S, L, O> path, IEsopEnv.Filter<S, L, O, P> filter,
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
                        Map<L, IEsopEnv<S, L, O, P>> env_lCache) {
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
                    Map<L, IEsopEnv<S, L, O, P>> env_lCache) {
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

    // serialization

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initTransients();
    }

}