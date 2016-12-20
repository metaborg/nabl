package org.metaborg.meta.nabl2.scopegraph.esop;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.regexp.IRegExp;
import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.regexp.RegExpMatcher;
import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.RelationDescription;
import org.metaborg.meta.nabl2.relations.terms.Relation;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IPath;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.terms.Paths;
import org.metaborg.meta.nabl2.util.Optionals;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.meta.nabl2.util.functions.PartialFunction0;
import org.metaborg.util.iterators.Iterables2;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class EsopNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence> implements
        INameResolution<S,L,O> {

    private final EsopScopeGraph<S,L,O> scopeGraph;
    private final Set<L> labels;
    private final IRegExp<L> wf;
    private final IRegExp<L> noWf;
    private final IRelation<L> order;
    private final IRelation<L> noOrder;

    private final Map<O,Iterable<IPath<S,L,O>>> resolveCache;

    public EsopNameResolution(EsopScopeGraph<S,L,O> scopeGraph, IResolutionParameters<L> params) {
        this.scopeGraph = scopeGraph;
        this.labels = Sets.newHashSet(params.getLabels());
        this.wf = params.getPathWf();
        this.order = params.getSpecificityOrder();
        assert order.getDescription().equals(
                RelationDescription.STRICT_PARTIAL_ORDER) : "Label specificity order must be a strict partial order";
        this.noWf = wf.getBuilder().complement(wf.getBuilder().emptySet());
        this.noOrder = new Relation<>(RelationDescription.STRICT_PARTIAL_ORDER);
        this.resolveCache = Maps.newHashMap();
    }

    @Override public Iterable<IPath<S,L,O>> resolve(O ref) {
        return tryResolve(ref).orElseGet(() -> Iterables2.empty());
    }

    @Override public Iterable<IPath<S,L,O>> visible(S scope) {
        return tryVisible(scope).orElseGet(() -> Iterables2.empty());
    }

    @Override public Iterable<IPath<S,L,O>> reachable(S scope) {
        return tryReachable(scope).orElseGet(() -> Iterables2.empty());
    }

    public Optional<Iterable<IPath<S,L,O>>> tryResolve(O ref) {
        if (resolveCache.containsKey(ref)) {
            return Optional.of(resolveCache.get(ref));
        } else {
            Optional<Iterable<IPath<S,L,O>>> paths = resolve(HashTreePSet.empty(), ref);
            paths.ifPresent(ds -> resolveCache.put(ref, ds));
            return paths;
        }
    }

    public Optional<Iterable<IPath<S,L,O>>> tryVisible(S scope) {
        EsopEnv<S,L,O> env = env(HashTreePSet.empty(), HashTreePSet.empty(), order, RegExpMatcher.create(wf), scope);
        return env.isComplete() ? Optional.of(env.getAll()) : Optional.empty();
    }

    public Optional<Iterable<IPath<S,L,O>>> tryReachable(S scope) {
        EsopEnv<S,L,O> env = env(HashTreePSet.empty(), HashTreePSet.empty(), noOrder, RegExpMatcher.create(noWf),
                scope);
        return env.isComplete() ? Optional.of(env.getAll()) : Optional.empty();
    }

    private Optional<Iterable<IPath<S,L,O>>> resolve(PSet<O> seenI, O ref) {
        return scopeGraph.getRefScope(ref).map(scope -> {
            EsopEnv<S,L,O> e = env(seenI.plus(ref), HashTreePSet.empty(), order, RegExpMatcher.create(wf), scope);
            Iterable<IPath<S,L,O>> paths = e.get(ref);
            if (e.isComplete()) {
                return Optional.of(paths);
            } else {
                return Iterables.isEmpty(paths) ? Optional.<Iterable<IPath<S,L,O>>> empty() : Optional.of(paths);
            }
        }).orElseGet(() -> Optional.of(Iterables2.empty()));
    }

    private EsopEnv<S,L,O> env(PSet<O> seenImports, PSet<S> seenScopes, IRelation<L> lt, IRegExpMatcher<L> re,
            S scope) {
        if (seenScopes.contains(scope) || re.isEmpty()) {
            return EsopEnv.empty(true);
        }
        return env_L(labels, seenImports, seenScopes, lt, re, scope);
    }

    private EsopEnv<S,L,O> env_l(PSet<O> seenImports, PSet<S> seenScopes, IRelation<L> lt, IRegExpMatcher<L> re, L l,
            S scope) {
        return l.getName().equals("D") ? env_D(seenImports, seenScopes, lt, re, l, scope)
                : env_nonD(seenImports, seenScopes, lt, re, l, scope);
    }

    private EsopEnv<S,L,O> env_D(PSet<O> seenImports, PSet<S> seenScopes, IRelation<L> lt, IRegExpMatcher<L> re, L l,
            S scope) {
        if (!re.isAccepting()) {
            return EsopEnv.empty(true);
        }
        List<IPath<S,L,O>> paths = Lists.newArrayList();
        for (O decl : scopeGraph.getDecls(scope)) {
            paths.add(Paths.decl(scope, decl));
        }
        return EsopEnv.of(true, paths);
    }

    private EsopEnv<S,L,O> env_nonD(PSet<O> seenImports, PSet<S> seenScopes, IRelation<L> lt, IRegExpMatcher<L> re, L l,
            S scope) {
        Function1<S,EsopEnv<S,L,O>> getter = s -> env(seenImports, seenScopes.plus(scope), lt, re.match(l), s);
        Optional<Iterable<EsopEnv<S,L,O>>> ods = directScopes(l, scope, getter);
        Optional<Iterable<EsopEnv<S,L,O>>> ois = importScopes(seenImports, l, scope, getter);
        return Optionals.lift(ods, ois, (ds, is) -> {
            EsopEnv<S,L,O> env = EsopEnv.empty(true);
            for (EsopEnv<S,L,O> nextEnv : Iterables.concat(ds, is)) {
                env.union(nextEnv);
            }
            return env;
        }).orElseGet(() -> EsopEnv.empty(false));
    }

    private Optional<Iterable<EsopEnv<S,L,O>>> directScopes(L l, S scope, Function1<S,EsopEnv<S,L,O>> getter) {
        List<EsopEnv<S,L,O>> envs = Lists.newArrayList();
        for (PartialFunction0<S> nextScope : scopeGraph.getDirectEdges(scope).get(l)) {
            Optional<S> maybeScope = nextScope.apply();
            if (!maybeScope.isPresent()) {
                return Optional.empty();
            }
            EsopEnv<S,L,O> env = getter.apply(maybeScope.get());
            env.map(p -> Paths.direct(scope, l, p));
            envs.add(env);
        }
        return Optional.of(envs);
    }

    private Optional<Iterable<EsopEnv<S,L,O>>> importScopes(PSet<O> seenImports, L l, S scope,
            Function1<S,EsopEnv<S,L,O>> getter) {
        List<EsopEnv<S,L,O>> envs = Lists.newArrayList();
        for (PartialFunction0<O> getRef : scopeGraph.getImports(scope).get(l)) {
            Optional<O> maybeRef = getRef.apply();
            if (!maybeRef.isPresent()) {
                return Optional.empty();
            }
            O ref = maybeRef.get();
            if (seenImports.contains(ref)) {
                continue;
            }
            Optional<Iterable<IPath<S,L,O>>> paths = resolve(seenImports, ref);
            if (!paths.isPresent()) {
                return Optional.empty();
            }
            for (IPath<S,L,O> path : paths.get()) {
                for (S nextScope : scopeGraph.getAssocScopes(path.getDeclaration()).get(l)) {
                    EsopEnv<S,L,O> env = getter.apply(nextScope);
                    env.map(p -> Paths.named(scope, l, ref, path, p));
                    envs.add(env);
                }
            }
        }
        return Optional.of(envs);
    }

    // stage environment shadowing call tree

    private EsopEnv<S,L,O> env_L(Set<L> L, PSet<O> seenImports, PSet<S> seenScopes, IRelation<L> lt,
            IRegExpMatcher<L> re, S scope) {
        EsopEnv<S,L,O> env = EsopEnv.empty(true);
        for (L l : max(lt, L)) {
            EsopEnv<S,L,O> partialEnv = env_L(smaller(lt, L, l), seenImports, seenScopes, lt, re, scope);
            partialEnv.shadow(env_l(seenImports, seenScopes, lt, re, l, scope));
            env.union(partialEnv);
        }
        return env;
    }

    private Set<L> max(IRelation<L> lt, Set<L> L) {
        Set<L> maxL = Sets.newHashSet();
        tryNext: for (L l : L) {
            for (L larger : lt.larger(l)) {
                if (L.contains(larger)) {
                    continue tryNext;
                }
            }
            maxL.add(l);
        }
        return maxL;
    }

    private Set<L> smaller(IRelation<L> lt, Set<L> L, L l) {
        Set<L> smallerL = Sets.newHashSet();
        for (L smaller : lt.smaller(l)) {
            if (L.contains(smaller)) {
                smallerL.add(smaller);
            }
        }
        return smallerL;
    }

}