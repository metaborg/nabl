package org.metaborg.meta.nabl2.scopegraph.esop;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.regexp.RegExpMatcher;
import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.RelationDescription;
import org.metaborg.meta.nabl2.relations.terms.Relation;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.RefCounter;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.Optionals;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class EsopNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
    implements INameResolution<S, L, O>, Serializable {

    private static final long serialVersionUID = 42L;

    private static final boolean useStaging = true;

    private final EsopScopeGraph<S, L, O> scopeGraph;
    private final Set<L> labels;
    private final L labelD;
    private final IRegExpMatcher<L> wf;
    private final IRelation<L> order;
    private final IRelation<L> noOrder;

    private final RefCounter<S, L> scopeCounter;
    private final RefCounter<O, L> assocCounter;

    transient private Map<O, Iterable<IResolutionPath<S, L, O>>> resolveCache;
    transient private Map<IRelation<L>, EnvL<S, L, O>> stagedEnv_L;

    public EsopNameResolution(EsopScopeGraph<S, L, O> esopScopeGraph, IResolutionParameters<L> params,
        RefCounter<S, L> scopeCounter, RefCounter<O, L> assocCounter) {
        this.scopeGraph = scopeGraph;
        this.labels = Sets.newHashSet(params.getLabels());
        this.labelD = params.getLabelD();
        this.wf = RegExpMatcher.create(params.getPathWf());
        this.order = params.getSpecificityOrder();
        assert order.getDescription().equals(
            RelationDescription.STRICT_PARTIAL_ORDER) : "Label specificity order must be a strict partial order";
        this.noOrder = new Relation<>(RelationDescription.STRICT_PARTIAL_ORDER);
        this.scopeCounter = scopeCounter;
        this.assocCounter = assocCounter;
        initTransients();
    }

    private void initTransients() {
        this.resolveCache = Maps.newHashMap();
        this.stagedEnv_L = Maps.newHashMap();
    }

    @Override public Iterable<S> getAllScopes() {
        return scopeGraph.getAllScopes();
    }

    @Override public Iterable<O> getAllRefs() {
        return scopeGraph.getAllRefs();
    }

    @Override public Iterable<IResolutionPath<S, L, O>> resolve(O ref) {
        return tryResolve(ref).orElseGet(() -> Iterables2.empty());
    }

    @Override public Iterable<IDeclPath<S, L, O>> visible(S scope) {
        return tryVisible(scope).orElseGet(() -> Iterables2.empty());
    }

    @Override public Iterable<IDeclPath<S, L, O>> reachable(S scope) {
        return tryReachable(scope).orElseGet(() -> Iterables2.empty());
    }

    public Optional<Iterable<IResolutionPath<S, L, O>>> tryResolve(O ref) {
        if(resolveCache.containsKey(ref)) {
            return Optional.of(resolveCache.get(ref));
        } else {
            Optional<Iterable<IResolutionPath<S, L, O>>> paths = resolve(HashTreePSet.empty(), ref);
            paths.ifPresent(ds -> resolveCache.put(ref, ds));
            return paths;
        }
    }

    public Optional<Iterable<IDeclPath<S, L, O>>> tryVisible(S scope) {
        EsopEnv<S, L, O> env = env(HashTreePSet.empty(), HashTreePSet.empty(), order, wf, scope);
        return env.isComplete() ? Optional.of(env.getAll()) : Optional.empty();
    }

    public Optional<Iterable<IDeclPath<S, L, O>>> tryReachable(S scope) {
        EsopEnv<S, L, O> env = env(HashTreePSet.empty(), HashTreePSet.empty(), noOrder, wf, scope);
        return env.isComplete() ? Optional.of(env.getAll()) : Optional.empty();
    }

    private Optional<Iterable<IResolutionPath<S, L, O>>> resolve(PSet<O> seenI, O ref) {
        return scopeGraph.getRefs().get(ref).map(scope -> {
            EsopEnv<S, L, O> e = env(seenI.plus(ref), HashTreePSet.empty(), order, wf, scope);
            Iterable<IResolutionPath<S, L, O>> paths = e.get(ref);
            if(e.isComplete()) {
                return Optional.of(paths);
            } else {
                return Iterables.isEmpty(paths) ? Optional.<Iterable<IResolutionPath<S, L, O>>>empty()
                    : Optional.of(paths);
            }
        }).orElseGet(() -> Optional.of(Iterables2.empty()));
    }

    private EsopEnv<S, L, O> env(PSet<O> seenImports, PSet<S> seenScopes, IRelation<L> lt, IRegExpMatcher<L> re,
        S scope) {
        if(seenScopes.contains(scope) || re.isEmpty()) {
            return EsopEnv.empty(true);
        }
        scopeGraph.freezeScope(scope);
        return env_L(labels, seenImports, seenScopes, lt, re, scope);
    }

    private EsopEnv<S, L, O> env_l(PSet<O> seenImports, PSet<S> seenScopes, IRelation<L> lt, IRegExpMatcher<L> re, L l,
        S scope) {
        if(scopeGraph.isEdgeOpen(scope, l)) {
            return EsopEnv.empty(false);
        }
        scopeGraph.freezeEdge(scope, l);
        return l.equals(labelD) ? env_D(seenImports, seenScopes, lt, re, scope)
            : env_nonD(seenImports, seenScopes, lt, re, l, scope);
    }

    private EsopEnv<S, L, O> env_D(PSet<O> seenImports, PSet<S> seenScopes, IRelation<L> lt, IRegExpMatcher<L> re,
        S scope) {
        if(scopeGraph.isEdgeOpen(scope, labelD)) {
            return EsopEnv.empty(false);
        }
        if(!re.isAccepting()) {
            return EsopEnv.empty(true);
        }
        List<IDeclPath<S, L, O>> paths = Lists.newArrayList();
        for(O decl : scopeGraph.getDecls().inverse().get(scope)) {
            paths.add(Paths.decl(Paths.empty(scope), decl));
        }
        return EsopEnv.of(true, paths);
    }

    private EsopEnv<S, L, O> env_nonD(PSet<O> seenImports, PSet<S> seenScopes, IRelation<L> lt, IRegExpMatcher<L> re,
        L l, S scope) {
        Function1<S, EsopEnv<S, L, O>> getter = s -> env(seenImports, seenScopes.plus(scope), lt, re.match(l), s);
        Optional<Iterable<EsopEnv<S, L, O>>> ods = directScopes(l, scope, getter);
        Optional<Iterable<EsopEnv<S, L, O>>> ois = importScopes(seenImports, l, scope, getter);
        return Optionals.lift(ods, ois, (ds, is) -> {
            EsopEnv<S, L, O> env = EsopEnv.empty(true);
            for(EsopEnv<S, L, O> nextEnv : Iterables.concat(ds, is)) {
                env.union(nextEnv);
            }
            return env;
        }).orElseGet(() -> EsopEnv.empty(false));
    }

    private Optional<Iterable<EsopEnv<S, L, O>>> directScopes(L l, S scope, Function1<S, EsopEnv<S, L, O>> getter) {
        List<EsopEnv<S, L, O>> envs = Lists.newArrayList();
        for(S nextScope : scopeGraph.getDirectEdges().get(scope, l)) {
            EsopEnv<S, L, O> env = getter.apply(nextScope);
            env.filter(p -> Paths.append(Paths.direct(scope, l, nextScope), p));
            envs.add(env);
        }
        return Optional.of(envs);
    }

    private Optional<Iterable<EsopEnv<S, L, O>>> importScopes(PSet<O> seenImports, L l, S scope,
        Function1<S, EsopEnv<S, L, O>> getter) {
        List<EsopEnv<S, L, O>> envs = Lists.newArrayList();
        for(O ref : scopeGraph.getImportEdges().get(scope, l)) {
            if(seenImports.contains(ref)) {
                continue;
            }
            Optional<Iterable<IResolutionPath<S, L, O>>> paths = resolve(seenImports, ref);
            if(!paths.isPresent()) {
                return Optional.empty();
            }
            for(IResolutionPath<S, L, O> path : paths.get()) {
                O decl = path.getDeclaration();
                scopeGraph.freezeEdge(decl, l);
                for(S nextScope : scopeGraph.getAssocEdges().get(decl, l)) {
                    EsopEnv<S, L, O> env = getter.apply(nextScope);
                    env.filter(p -> Paths.append(Paths.named(scope, l, path, nextScope), p));
                    envs.add(env);
                }
            }
        }
        return Optional.of(envs);
    }

    private EsopEnv<S, L, O> env_L(Set<L> L, PSet<O> seenImports, PSet<S> seenScopes, IRelation<L> lt,
        IRegExpMatcher<L> re, S scope) {
        return useStaging
            ? stagedEnv_L.computeIfAbsent(lt, lo -> stageEnv_L(L, lt)).apply(seenImports, seenScopes, re, scope)
            : unstagedEnv_L(L, seenImports, seenScopes, lt, re, scope);
    }

    private EsopEnv<S, L, O> unstagedEnv_L(Set<L> L, PSet<O> seenImports, PSet<S> seenScopes, IRelation<L> lt,
        IRegExpMatcher<L> re, S scope) {
        EsopEnv<S, L, O> env = EsopEnv.empty(true);
        for(L l : max(lt, L)) {
            EsopEnv<S, L, O> partialEnv = env_L(smaller(lt, L, l), seenImports, seenScopes, lt, re, scope);
            partialEnv.shadow(env_l(seenImports, seenScopes, lt, re, l, scope));
            env.union(partialEnv);
        }
        return env;
    }

    private EnvL<S, L, O> stageEnv_L(Set<L> L, IRelation<L> lt) {
        List<EnvL<S, L, O>> stagedEnvs = Lists.newArrayList();
        for(L l : max(lt, L)) {
            EnvL<S, L, O> smallerEnv = stageEnv_L(smaller(lt, L, l), lt);
            stagedEnvs.add((seenImports, seenScopes, re, scope) -> {
                EsopEnv<S, L, O> env = smallerEnv.apply(seenImports, seenScopes, re, scope);
                env.shadow(env_l(seenImports, seenScopes, lt, re, l, scope));
                return env;
            });
        }
        return (seenImports, seenScopes, re, scope) -> {
            EsopEnv<S, L, O> env = EsopEnv.empty(true);
            for(EnvL<S, L, O> stagedEnv : stagedEnvs) {
                env.union(stagedEnv.apply(seenImports, seenScopes, re, scope));
            }
            return env;
        };
    }

    private Set<L> max(IRelation<L> lt, Set<L> L) {
        Set<L> maxL = Sets.newHashSet();
        tryNext: for(L l : L) {
            for(L larger : lt.larger(l)) {
                if(L.contains(larger)) {
                    continue tryNext;
                }
            }
            maxL.add(l);
        }
        return maxL;
    }

    private Set<L> smaller(IRelation<L> lt, Set<L> L, L l) {
        Set<L> smallerL = Sets.newHashSet();
        for(L smaller : lt.smaller(l)) {
            if(L.contains(smaller)) {
                smallerL.add(smaller);
            }
        }
        return smallerL;
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