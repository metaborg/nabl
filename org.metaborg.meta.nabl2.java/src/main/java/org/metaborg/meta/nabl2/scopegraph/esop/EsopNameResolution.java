package org.metaborg.meta.nabl2.scopegraph.esop;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.functions.Function4;
import org.metaborg.meta.nabl2.functions.PartialFunction0;
import org.metaborg.meta.nabl2.regexp.IRegExp;
import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.regexp.RegExpMatcher;
import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
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
    private final IRelation<L> order;
    private final Function4<PSet<O>,PSet<S>,IRegExpMatcher<L>,S,EsopEnv<O>> stagedEnv_L;

    private final Map<O,Iterable<O>> resolveCache;

    public EsopNameResolution(EsopScopeGraph<S,L,O> scopeGraph, IResolutionParameters<L> params) {
        this.scopeGraph = scopeGraph;
        this.labels = Sets.newHashSet(params.getLabels());
        this.wf = params.getPathWf();
        this.order = params.getSpecificityOrder();
        this.stagedEnv_L = stageEnv_L(labels);
        this.resolveCache = Maps.newHashMap();
    }

    @Override public Iterable<O> resolve(O ref) {
        return tryResolve(ref).orElseGet(() -> Iterables2.empty());
    }

    public Optional<Iterable<O>> tryResolve(O ref) {
        if (resolveCache.containsKey(ref)) {
            return Optional.of(resolveCache.get(ref));
        } else {
            Optional<Iterable<O>> decls = resolve(HashTreePSet.empty(), ref);
            decls.ifPresent(ds -> resolveCache.put(ref, ds));
            return decls;
        }
    }

    private Optional<Iterable<O>> resolve(PSet<O> seenI, O ref) {
        return scopeGraph.getRefScope(ref).map(scope -> {
            EsopEnv<O> e = env(seenI.plus(ref), HashTreePSet.empty(), RegExpMatcher.create(wf), scope);
            Iterable<O> decls = e.get(ref);
            if (e.isComplete()) {
                return Optional.of(decls);
            } else {
                return Iterables.isEmpty(decls) ? Optional.<Iterable<O>> empty() : Optional.of(decls);
            }
        }).orElseGet(() -> Optional.of(Iterables2.empty()));
    }

    private EsopEnv<O> env(PSet<O> seenImports, PSet<S> seenScopes, IRegExpMatcher<L> re, S scope) {
        if (seenScopes.contains(scope) || re.isEmpty()) {
            return EsopEnv.empty(true);
        }
        // return env_L(labels, seenImports, seenScopes, re, scope);
        return stagedEnv_L.apply(seenImports, seenScopes, re, scope);
    }

    private EsopEnv<O> env_l(PSet<O> seenImports, PSet<S> seenScopes, IRegExpMatcher<L> re, L l, S scope) {
        if (l.getName().equals("D")) {
            if (!re.isAccepting()) {
                return EsopEnv.empty(true);
            }
            Iterable<O> decls = scopeGraph.getDecls(scope);
            return EsopEnv.of(true, decls);
        }
        Optional<Iterable<S>> ds = directScopes(l, scope);
        Optional<Iterable<S>> is = importScopes(seenImports, l, scope);
        if (!(ds.isPresent() && is.isPresent())) {
            return EsopEnv.empty(false);
        }
        EsopEnv<O> env = EsopEnv.empty(true);
        for (S nextScope : Iterables.concat(ds.get(), is.get())) {
            env.union(env(seenImports, seenScopes.plus(scope), re.match(l), nextScope));
        }
        return env;
    }

    private Optional<Iterable<S>> directScopes(L l, S scope) {
        List<S> scopes = Lists.newArrayList();
        for (PartialFunction0<S> getScope : scopeGraph.getDirectEdges(scope, l)) {
            Optional<S> maybeScope = getScope.apply();
            if (!maybeScope.isPresent()) {
                return Optional.empty();
            }
            scopes.add(maybeScope.get());
        }
        return Optional.of(scopes);
    }

    private Optional<Iterable<S>> importScopes(PSet<O> seenImports, L l, S scope) {
        List<S> scopes = Lists.newArrayList();
        for (PartialFunction0<O> getRef : scopeGraph.getImports(scope, l)) {
            Optional<O> maybeRef = getRef.apply();
            if (!maybeRef.isPresent()) {
                return Optional.empty();
            }
            O ref = maybeRef.get();
            if (seenImports.contains(ref)) {
                continue;
            }
            Optional<Iterable<O>> decls = resolve(seenImports, ref);
            if (!decls.isPresent()) {
                return Optional.empty();
            }
            for (O decl : decls.get()) {
                for (S nextScope : scopeGraph.getAssocs(decl, l)) {
                    scopes.add(nextScope);
                }
            }
        }
        return Optional.of(scopes);
    }

    // stage environment shadowing call tree

    @SuppressWarnings("unused") private EsopEnv<O> env_L(Set<L> L, PSet<O> seenImports, PSet<S> seenScopes,
            IRegExpMatcher<L> re, S scope) {
        EsopEnv<O> env = EsopEnv.empty(true);
        for (L l : max(L)) {
            EsopEnv<O> partialEnv = env_L(smaller(L, l), seenImports, seenScopes, re, scope);
            partialEnv.shadow(env_l(seenImports, seenScopes, re, l, scope));
            env.union(partialEnv);
        }
        return env;
    }

    private Function4<PSet<O>,PSet<S>,IRegExpMatcher<L>,S,EsopEnv<O>> stageEnv_L(Set<L> L) {
        List<Function4<PSet<O>,PSet<S>,IRegExpMatcher<L>,S,EsopEnv<O>>> stagedEnvs = Lists.newArrayList();
        for (L l : max(L)) {
            Function4<PSet<O>,PSet<S>,IRegExpMatcher<L>,S,EsopEnv<O>> smallerEnv = stageEnv_L(smaller(L, l));
            stagedEnvs.add((seenImports, seenScopes, re, scope) -> {
                EsopEnv<O> env = smallerEnv.apply(seenImports, seenScopes, re, scope);
                env.shadow(env_l(seenImports, seenScopes, re, l, scope));
                return env;
            });
        }
        return (seenImports, seenScopes, re, scope) -> {
            EsopEnv<O> env = EsopEnv.empty(true);
            for (Function4<PSet<O>,PSet<S>,IRegExpMatcher<L>,S,EsopEnv<O>> stagedEnv : stagedEnvs) {
                env.union(stagedEnv.apply(seenImports, seenScopes, re, scope));
            }
            return env;
        };
    }

    private Set<L> max(Set<L> L) {
        Set<L> maxL = Sets.newHashSet();
        tryNext: for (L l : L) {
            for (L larger : order.larger(l)) {
                if (L.contains(larger)) {
                    continue tryNext;
                }
            }
            maxL.add(l);
        }
        return maxL;
    }

    private Set<L> smaller(Set<L> L, L l) {
        Set<L> smallerL = Sets.newHashSet();
        for (L smaller : order.smaller(l)) {
            if (L.contains(smaller)) {
                smallerL.add(smaller);
            }
        }
        return smallerL;
    }

}