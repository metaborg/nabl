package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

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
import org.metaborg.meta.nabl2.scopegraph.OpenCounter;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.functions.Function0;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.meta.nabl2.util.functions.PartialFunction0;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;

public class PersistentNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution<S, L, O>, Serializable {

    private static final long serialVersionUID = 42L;

    private final PersistentScopeGraph<S, L, O> scopeGraph;

    private final Set<L> labels;
    private final L labelD;
    private final IRegExpMatcher<L> wf;
    private final IRelation<L> order;
    private final IRelation<L> noOrder;

    private final OpenCounter<S, L> scopeCounter;

    transient private Map<O, IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>>> resolveCache;

    transient private Map<S, IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>>> visibleCache;
    transient private Map<S, IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>>> reachableCache;
    
    transient private Map<IRelation<L>, EnvironmentL<S, L, O>> stagedEnv_L;

    public PersistentNameResolution(PersistentScopeGraph<S, L, O> scopeGraph, IResolutionParameters<L> params,
            OpenCounter<S, L> scopeCounter) {
        this.scopeGraph = scopeGraph;
        
        this.labels = Set.Immutable.<L>of().__insertAll(Sets.newHashSet(params.getLabels()));
        this.labelD = params.getLabelD();
        this.wf = RegExpMatcher.create(params.getPathWf());
        this.order = params.getSpecificityOrder();
        assert order.getDescription().equals(
                RelationDescription.STRICT_PARTIAL_ORDER) : "Label specificity order must be a strict partial order";
        this.noOrder = new Relation<>(RelationDescription.STRICT_PARTIAL_ORDER);
        this.scopeCounter = scopeCounter;

        initTransients();
    }

    private void initTransients() {
        this.resolveCache = Maps.newHashMap();
        this.visibleCache = Maps.newHashMap();
        this.reachableCache = Maps.newHashMap();
        this.stagedEnv_L = Maps.newHashMap();
    }

    // NOTE: never used in project
    @Override
    public Set.Immutable<S> getAllScopes() {
        return scopeGraph.getAllScopes();
    }

    // NOTE: all references could be duplicated to get rid of scope graph reference
    @Override
    public Set.Immutable<O> getAllRefs() {
        return scopeGraph.getAllRefs();
    }

    @Override
    public Set.Immutable<IResolutionPath<S, L, O>> resolve(O ref) {
        return tryResolve(ref).map(Tuple2::_1).orElse(Set.Immutable.of());
    }

    @Override
    public Set.Immutable<IDeclPath<S, L, O>> visible(S scope) {
        return tryVisible(scope).map(Tuple2::_1).orElse(Set.Immutable.of());
    }

    @Override
    public Set.Immutable<IDeclPath<S, L, O>> reachable(S scope) {
        return tryReachable(scope).map(Tuple2::_1).orElse(Set.Immutable.of());
    }

    public Optional<Tuple2<Immutable<IResolutionPath<S, L, O>>, Immutable<String>>> tryResolve(O ref) {
        final IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> env = resolveCache.computeIfAbsent(ref,
                r -> resolveEnv(Set.Immutable.of(), ref));
        return env.getAll().map(ps -> ImmutableTuple2.of(ps, Set.Immutable.of()));
    }

    public Optional<Tuple2<Immutable<IDeclPath<S, L, O>>, Immutable<String>>> tryVisible(S scope) {
        final IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> env = visibleCache.computeIfAbsent(scope,
                s -> visibleEnv(scope));
        return env.getAll().map(ps -> ImmutableTuple2.of(ps, Set.Immutable.of()));
    }

    public Optional<Tuple2<Immutable<IDeclPath<S, L, O>>, Immutable<String>>> tryReachable(S scope) {
        final IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> env = reachableCache.computeIfAbsent(scope,
                s -> reachableEnv(scope));
        return env.getAll().map(ps -> ImmutableTuple2.of(ps, Set.Immutable.of()));
    }

    private IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> visibleEnv(S scope) {
        return env(Set.Immutable.of(), order, wf, Paths.empty(scope), Environments.envFilter());
    }

    private IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> reachableEnv(S scope) {
        return env(Set.Immutable.of(), noOrder, wf, Paths.empty(scope), Environments.envFilter());
    }

    private IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> resolveEnv(Set.Immutable<O> seenI, O ref) {
        return scopeGraph.getRefs().get(ref).map(
                scope -> env(seenI.__insert(ref), order, wf, Paths.empty(scope), Environments.resolutionFilter(ref)))
                .orElseGet(() -> Environments.empty());
    }

    private <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> env(Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter) {
        if (re.isEmpty()) {
            return Environments.empty();
        } else {
            return env_L(labels, seenImports, lt, re, path, filter);
        }
    }

    private <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> env_l(Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, L l, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter) {
        return Environments.guarded((PartialFunction0<IPersistentEnvironment<S, L, O, P>> & Serializable) () -> {
            if (scopeCounter.isOpen(path.getTarget(), l)) {
                return Optional.empty();
            } else {
                final IPersistentEnvironment<S, L, O, P> env = l.equals(labelD)
                        ? env_D(seenImports, lt, re, path, filter) : env_nonD(seenImports, lt, re, l, path, filter);
                return Optional.of(env);
            }
        });
    }

    private <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> env_D(Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter) {
        if (!re.isAccepting()) {
            return Environments.empty();
        } else {
            List<P> paths = Lists.newArrayList();
            for (O decl : scopeGraph.getDecls().inverse().get(path.getTarget())) {
                filter.test(Paths.decl(path, decl)).ifPresent(paths::add);
            }
            return Environments.init(paths);
        }
    }

    private <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> env_nonD(Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, L l, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter) {
        Function1<IScopePath<S, L, O>, IPersistentEnvironment<S, L, O, P>> getter = (Function1<IScopePath<S, L, O>, IPersistentEnvironment<S, L, O, P>> & Serializable) p -> {
            return env(seenImports, lt, re.match(l), p, filter);
        };
        return Environments.union(Iterables.concat(directScopes(l, path, filter, getter),
                importScopes(seenImports, l, path, filter, getter)));
    }

    private <P extends IPath<S, L, O>> Iterable<IPersistentEnvironment<S, L, O, P>> directScopes(L l,
            IScopePath<S, L, O> path, IPersistentEnvironment.Filter<S, L, O, P> filter,
            Function1<IScopePath<S, L, O>, IPersistentEnvironment<S, L, O, P>> getter) {
        List<IPersistentEnvironment<S, L, O, P>> envs = Lists.newArrayList();
        for (S nextScope : scopeGraph.getDirectEdges().get(path.getTarget(), l)) {
            Paths.append(path, Paths.direct(path.getTarget(), l, nextScope)).map(getter::apply).ifPresent(envs::add);
        }
        return envs;
    }

    private <P extends IPath<S, L, O>> Iterable<IPersistentEnvironment<S, L, O, P>> importScopes(
            Set.Immutable<O> seenImports, L l, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter,
            Function1<IScopePath<S, L, O>, IPersistentEnvironment<S, L, O, P>> getter) {
        List<IPersistentEnvironment<S, L, O, P>> envs = Lists.newArrayList();
        for (O ref : scopeGraph.getImportEdges().get(path.getTarget(), l)) {
            if (seenImports.contains(ref)) {
                continue;
            }
            final IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> env = resolveEnv(seenImports, ref);
            envs.add(Environments.guarded((PartialFunction0<IPersistentEnvironment<S, L, O, P>> & Serializable) () -> {
                return env.getAll().map(paths -> {
                    List<IPersistentEnvironment<S, L, O, P>> importEnvs = Lists.newArrayList();
                    for (IResolutionPath<S, L, O> importPath : paths) {
                        for (S nextScope : scopeGraph.getExportEdges().get(importPath.getDeclaration(), l)) {
                            Paths.append(path, Paths.named(path.getTarget(), l, importPath, nextScope))
                                    .map(getter::apply).ifPresent(importEnvs::add);
                        }
                    }
                    return Environments.union(importEnvs);
                });
            }));

        }
        return envs;
    }

    private <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> env_L(Set<L> L, Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter) {
        return stagedEnv_L.computeIfAbsent(lt, lo -> stageEnv_L(L, lt)).apply(seenImports, re, path, filter,
                Maps.newHashMap());
    }

    private EnvironmentL<S, L, O> stageEnv_L(Set<L> L, IRelation<L> lt) {
        List<EnvironmentL<S, L, O>> stagedEnvs = Lists.newArrayList();
        for (L l : max(lt, L)) {
            EnvironmentL<S, L, O> smallerEnv = stageEnv_L(smaller(lt, L, l), lt);
            stagedEnvs.add(new EnvironmentL<S, L, O>() {

                @Override
                public <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> apply(Set.Immutable<O> seenI,
                        IRegExpMatcher<L> re, IScopePath<S, L, O> path,
                        IPersistentEnvironment.Filter<S, L, O, P> filter,
                        Map<L, IPersistentEnvironment<S, L, O, P>> env_lCache) {
                    final IPersistentEnvironment<S, L, O, P> env_l = Environments
                            .lazy((Function0<IPersistentEnvironment<S, L, O, P>> & Serializable) () -> {
                                return env_lCache.computeIfAbsent(l, ll -> env_l(seenI, lt, re, l, path, filter));
                            });
                    return Environments.shadow(filter, smallerEnv.apply(seenI, re, path, filter, env_lCache), env_l);
                }

            });
        }
        return new EnvironmentL<S, L, O>() {

            @Override
            public <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> apply(Set.Immutable<O> seenI,
                    IRegExpMatcher<L> re, IScopePath<S, L, O> path, IPersistentEnvironment.Filter<S, L, O, P> filter,
                    Map<L, IPersistentEnvironment<S, L, O, P>> env_lCache) {
                return Environments.union(stagedEnvs.stream().map(se -> se.apply(seenI, re, path, filter, env_lCache))
                        .collect(Collectors.toList()));

            }

        };
    }

    private Set.Immutable<L> max(IRelation<L> lt, Set<L> L) {
        Set.Transient<L> maxL = Set.Transient.of();
        tryNext: for (L l : L) {
            for (L larger : lt.larger(l)) {
                if (L.contains(larger)) {
                    continue tryNext;
                }
            }
            maxL.__insert(l);
        }
        return maxL.freeze();
    }

    private Set.Immutable<L> smaller(IRelation<L> lt, Set<L> L, L l) {
        Set.Transient<L> smallerL = Set.Transient.of();
        for (L smaller : lt.smaller(l)) {
            if (L.contains(smaller)) {
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
