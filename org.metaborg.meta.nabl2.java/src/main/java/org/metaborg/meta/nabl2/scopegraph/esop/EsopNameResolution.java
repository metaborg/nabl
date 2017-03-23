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
import org.metaborg.meta.nabl2.scopegraph.OpenCounter;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
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

    private final OpenCounter<S, L> scopeCounter;

    transient private Map<O, Iterable<IResolutionPath<S, L, O>>> resolveCache;
    transient private Map<IRelation<L>, EnvL<S, L, O>> stagedEnv_L;

    public EsopNameResolution(EsopScopeGraph<S, L, O> scopeGraph, IResolutionParameters<L> params,
        OpenCounter<S, L> scopeCounter) {
        this.scopeGraph = scopeGraph;
        this.labels = Sets.newHashSet(params.getLabels());
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
        EsopEnv<S, L, O> env = env(HashTreePSet.empty(), order, wf, Paths.empty(scope));
        return env.isComplete() ? Optional.of(env.getAll()) : Optional.empty();
    }

    public Optional<Iterable<IDeclPath<S, L, O>>> tryReachable(S scope) {
        EsopEnv<S, L, O> env = env(HashTreePSet.empty(), noOrder, wf, Paths.empty(scope));
        return env.isComplete() ? Optional.of(env.getAll()) : Optional.empty();
    }

    private Optional<Iterable<IResolutionPath<S, L, O>>> resolve(PSet<O> seenI, O ref) {
        return scopeGraph.getRefs().get(ref).map(scope -> {
            EsopEnv<S, L, O> e = env(seenI.plus(ref), order, wf, Paths.empty(scope));
            Iterable<IResolutionPath<S, L, O>> paths = e.get(ref);
            if(e.isComplete()) {
                return Optional.of(paths);
            } else {
                return Iterables.isEmpty(paths) ? Optional.<Iterable<IResolutionPath<S, L, O>>>empty()
                    : Optional.of(paths);
            }
        }).orElseGet(() -> Optional.of(Iterables2.empty()));
    }

    private EsopEnv<S, L, O> env(PSet<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re, IScopePath<S, L, O> path) {
        if(re.isEmpty()) {
            return EsopEnv.empty(true);
        }
        return env_L(labels, seenImports, lt, re, path);
    }

    private EsopEnv<S, L, O> env_l(PSet<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re, L l,
        IScopePath<S, L, O> path) {
        if(scopeCounter.isOpen(path.getTarget(), l)) {
            return EsopEnv.empty(false);
        }
        return l.equals(labelD) ? env_D(seenImports, lt, re, path) : env_nonD(seenImports, lt, re, l, path);
    }

    private EsopEnv<S, L, O> env_D(PSet<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re,
        IScopePath<S, L, O> path) {
        if(!re.isAccepting()) {
            return EsopEnv.empty(true);
        }
        List<IDeclPath<S, L, O>> paths = Lists.newArrayList();
        for(O decl : scopeGraph.getDecls().inverse().get(path.getTarget())) {
            paths.add(Paths.decl(path, decl));
        }
        return EsopEnv.of(true, paths);
    }

    private EsopEnv<S, L, O> env_nonD(PSet<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re, L l,
        IScopePath<S, L, O> path) {
        Function1<IScopePath<S, L, O>, EsopEnv<S, L, O>> getter = p -> env(seenImports, lt, re.match(l), p);
        Optional<Iterable<EsopEnv<S, L, O>>> ods = directScopes(l, path, getter);
        Optional<Iterable<EsopEnv<S, L, O>>> ois = importScopes(seenImports, l, path, getter);
        return Optionals.lift(ods, ois, (ds, is) -> {
            EsopEnv<S, L, O> env = EsopEnv.empty(true);
            for(EsopEnv<S, L, O> nextEnv : Iterables.concat(ds, is)) {
                env.union(nextEnv);
            }
            return env;
        }).orElseGet(() -> EsopEnv.empty(false));
    }

    private Optional<Iterable<EsopEnv<S, L, O>>> directScopes(L l, IScopePath<S, L, O> path,
        Function1<IScopePath<S, L, O>, EsopEnv<S, L, O>> getter) {
        List<EsopEnv<S, L, O>> envs = Lists.newArrayList();
        for(S nextScope : scopeGraph.getDirectEdges().get(path.getTarget(), l)) {
            Paths.append(path, Paths.direct(path.getTarget(), l, nextScope)).map(getter::apply).ifPresent(envs::add);
        }
        return Optional.of(envs);
    }

    private Optional<Iterable<EsopEnv<S, L, O>>> importScopes(PSet<O> seenImports, L l, IScopePath<S, L, O> path,
        Function1<IScopePath<S, L, O>, EsopEnv<S, L, O>> getter) {
        List<EsopEnv<S, L, O>> envs = Lists.newArrayList();
        for(O ref : scopeGraph.getImportEdges().get(path.getTarget(), l)) {
            if(seenImports.contains(ref)) {
                continue;
            }
            Optional<Iterable<IResolutionPath<S, L, O>>> paths = resolve(seenImports, ref);
            if(!paths.isPresent()) {
                return Optional.empty();
            }
            for(IResolutionPath<S, L, O> importPath : paths.get()) {
                O decl = importPath.getDeclaration();
                for(S nextScope : scopeGraph.getAssocEdges().get(decl, l)) {
                    Paths.append(path, Paths.named(path.getTarget(), l, importPath, nextScope)).map(getter::apply).ifPresent(envs::add);
                }
            }
        }
        return Optional.of(envs);
    }

    private EsopEnv<S, L, O> env_L(Set<L> L, PSet<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re,
        IScopePath<S, L, O> path) {
        return useStaging ? stagedEnv_L.computeIfAbsent(lt, lo -> stageEnv_L(L, lt)).apply(seenImports, re, path)
            : unstagedEnv_L(L, seenImports, lt, re, path);
    }

    private EsopEnv<S, L, O> unstagedEnv_L(Set<L> L, PSet<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re,
        IScopePath<S, L, O> path) {
        EsopEnv<S, L, O> env = EsopEnv.empty(true);
        for(L l : max(lt, L)) {
            EsopEnv<S, L, O> partialEnv = env_L(smaller(lt, L, l), seenImports, lt, re, path);
            partialEnv.shadow(env_l(seenImports, lt, re, l, path));
            env.union(partialEnv);
        }
        return env;
    }

    private EnvL<S, L, O> stageEnv_L(Set<L> L, IRelation<L> lt) {
        List<EnvL<S, L, O>> stagedEnvs = Lists.newArrayList();
        for(L l : max(lt, L)) {
            EnvL<S, L, O> smallerEnv = stageEnv_L(smaller(lt, L, l), lt);
            stagedEnvs.add((seenImports, re, path) -> {
                EsopEnv<S, L, O> env = smallerEnv.apply(seenImports, re, path);
                env.shadow(env_l(seenImports, lt, re, l, path));
                return env;
            });
        }
        return (seenImports, re, path) -> {
            EsopEnv<S, L, O> env = EsopEnv.empty(true);
            for(EnvL<S, L, O> stagedEnv : stagedEnvs) {
                env.union(stagedEnv.apply(seenImports, re, path));
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