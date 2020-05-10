package mb.nabl2.scopegraph.esop.bottomup;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;

import org.metaborg.util.functions.Function3;

import com.google.common.collect.Queues;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.relations.IRelation;
import mb.nabl2.relations.RelationDescription;
import mb.nabl2.relations.impl.Relation;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.INameResolution;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.path.IPath;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.path.IScopePath;
import mb.nabl2.scopegraph.path.IStep;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.ResolutionParameters;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;

public class BUNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements INameResolution<S, L, O> {

    private final IEsopScopeGraph<S, L, O, ?> scopeGraph;
    private final Iterable<L> labels;
    private final L labelD;
    private final IRegExpMatcher<L> wf;
    private final IRelation<L> order;
    private final IRelation<L> noOrder;

    public BUNameResolution(IEsopScopeGraph<S, L, O, ?> scopeGraph, Iterable<L> labels, L labelD, IRegExp<L> wf,
            IRelation<L> order) {
        this.scopeGraph = scopeGraph;
        this.labels = labels;
        this.labelD = labelD;
        this.wf = RegExpMatcher.create(wf);
        this.order = order;
        this.noOrder = Relation.Immutable.of(RelationDescription.STRICT_PARTIAL_ORDER);
    }

    public static BUNameResolution<Scope, Label, Occurrence> of(IEsopScopeGraph<Scope, Label, Occurrence, ?> scopeGraph,
            ResolutionParameters params) {
        return new BUNameResolution<>(scopeGraph, params.getLabels(), params.getLabelD(), params.getPathWf(),
                params.getSpecificityOrder());
    }

    @Override public java.util.Set<O> getResolvedRefs() {
        return resolved.keySet();
    }

    @Override public Collection<IResolutionPath<S, L, O>> resolve(O ref) {
        return resolveEnv(ref);
    }

    @Override public Collection<O> decls(S scope) {
        return scopeGraph.getDecls().inverse().get(scope);
    }

    @Override public Collection<O> refs(S scope) {
        return scopeGraph.getRefs().inverse().get(scope);
    }

    @Override public Collection<O> visible(S scope) {
        return Paths.declPathsToDecls(visibleEnv(scope));
    }

    @Override public Collection<O> reachable(S scope) {
        return Paths.declPathsToDecls(reachableEnv(scope));
    }

    @Override public Collection<Map.Entry<O, Collection<IResolutionPath<S, L, O>>>> resolutionEntries() {
        return resolved.entrySet();
    }

    private Map.Transient<O, Collection<IResolutionPath<S, L, O>>> resolved = Map.Transient.of();
    private Map.Transient<S, Collection<IDeclPath<S, L, O>>> reachable = Map.Transient.of();
    private Map.Transient<S, Collection<IDeclPath<S, L, O>>> visible = Map.Transient.of();

    private SetMultimap.Transient<EnvKey, IDeclPath<S, L, O>> envPaths = SetMultimap.Transient.of();
    private SetMultimap.Transient<RefKey, IResolutionPath<S, L, O>> refPaths = SetMultimap.Transient.of();

    private SetMultimap.Transient<EnvKey, Tuple2<EnvKey, IStep<S, L, O>>> backedges = SetMultimap.Transient.of();
    private SetMultimap.Transient<RefKey, Tuple3<EnvKey, L, IRegExpMatcher<L>>> backimports =
            SetMultimap.Transient.of();
    private SetMultimap.Transient<EnvKey, RefKey> backrefs = SetMultimap.Transient.of();

    private final Set.Transient<EnvKey> initedEnvs = Set.Transient.of();
    private final Set.Transient<RefKey> initedRefs = Set.Transient.of();

    private Collection<IResolutionPath<S, L, O>> resolveEnv(O ref) {
        Collection<IResolutionPath<S, L, O>> paths = resolved.get(ref);
        if(paths == null) {
            final RefKey key = new RefKey(ref);
            new Compute(key).call();
            resolved.__put(ref, paths = filter(refPaths.get(key), order, this::compare));
        }
        return paths;
    }

    private Collection<IDeclPath<S, L, O>> visibleEnv(S scope) {
        Collection<IDeclPath<S, L, O>> paths = visible.get(scope);
        if(paths == null) {
            final EnvKey key = new EnvKey(scope, wf);
            new Compute(key).call();
            visible.__put(scope, paths = filter(envPaths.get(key), order, this::compare));
        }
        return paths;
    }

    private Collection<IDeclPath<S, L, O>> reachableEnv(S scope) {
        Collection<IDeclPath<S, L, O>> paths = reachable.get(scope);
        if(paths == null) {
            final EnvKey key = new EnvKey(scope, wf);
            new Compute(key).call();
            reachable.__put(scope, paths = filter(envPaths.get(key), noOrder, this::compare));
        }
        return paths;
    }

    private class Compute {

        private final Deque<Task> worklist = Queues.newArrayDeque();

        public Compute(RefKey ref) {
            worklist.add(new RefTask(ref, Collections.emptySet()));
        }

        public Compute(EnvKey env) {
            worklist.add(new EnvTask(env, Collections.emptySet()));
        }

        public void call() {

            while(!worklist.isEmpty()) {
                worklist.pop().call();
            }

        }

        abstract class Task {

            abstract void call();

        }

        private class EnvTask extends Task {

            public final EnvKey env;
            public final java.util.Collection<IDeclPath<S, L, O>> paths;

            public EnvTask(EnvKey env, Collection<IDeclPath<S, L, O>> paths) {
                this.env = env;
                this.paths = paths;
            }

            @Override void call() {
                initEnv(env);
                paths.forEach(p -> envPaths.__insert(env, p));
                for(RefKey ref : backrefs.get(env)) {
                    final Collection<IResolutionPath<S, L, O>> refPaths = paths.stream()
                            .flatMap(p -> Streams.stream(Paths.resolve(ref.ref, p))).collect(CapsuleCollectors.toSet());
                    worklist.push(new RefTask(ref, refPaths));
                }
                for(Tuple2<EnvKey, IStep<S, L, O>> env2 : backedges.get(env)) {
                    final Collection<IDeclPath<S, L, O>> env2Paths =
                            paths.stream().flatMap(p -> Streams.stream(Paths.append(env2._2(), p)))
                                    .collect(CapsuleCollectors.toSet());
                    worklist.push(new EnvTask(env2._1(), env2Paths));
                }
            }

        }

        private class RefTask extends Task {

            public final RefKey ref;
            public final java.util.Collection<IResolutionPath<S, L, O>> paths;

            public RefTask(RefKey ref, Collection<IResolutionPath<S, L, O>> paths) {
                this.ref = ref;
                this.paths = paths;
            }

            @Override void call() {
                initRef(ref);
                paths.forEach(p -> refPaths.__insert(ref, p));
                for(Tuple3<EnvKey, L, IRegExpMatcher<L>> entry : backimports.get(ref)) {
                    for(IResolutionPath<S, L, O> p : paths) {
                        for(S s : scopeGraph.getExportEdges().get(p.getDeclaration(), entry._2())) {
                            final EnvKey env = new EnvKey(s, entry._3());
                            initEnv(env);
                            addBackEdge(env, Paths.named(entry._1().scope, entry._2(), p, env.scope), entry._1());
                        }
                    }
                }
            }

        }

        private void initEnv(EnvKey env) {
            if(initedEnvs.contains(env)) {
                return;
            }
            initedEnvs.__insert(env);
            if(env.wf.isAccepting()) {
                final Collection<IDeclPath<S, L, O>> declPaths = scopeGraph.getDecls().inverse().get(env.scope).stream()
                        .map(d -> Paths.decl(Paths.<S, L, O>empty(env.scope), d)).collect(CapsuleCollectors.toSet());
                worklist.push(new EnvTask(env, declPaths));
            }
            for(L l : labels) {
                IRegExpMatcher<L> wf2 = wf.match(l);
                if(wf2.isEmpty()) {
                    continue;
                }
                for(S scope : scopeGraph.getDirectEdges().get(env.scope, l)) {
                    final EnvKey env2 = new EnvKey(scope, wf2);
                    initEnv(env2);
                    addBackEdge(env2, Paths.direct(env.scope, l, env2.scope), env);
                }
                for(O ref : scopeGraph.getImportEdges().get(env.scope, l)) {
                    final RefKey ref2 = new RefKey(ref);
                    initRef(ref2);
                    addBackImport(ref2, l, wf2, env);
                }
            }
        }

        private void initRef(RefKey ref) {
            if(initedRefs.contains(ref)) {
                return;
            }
            initedRefs.__insert(ref);
            scopeGraph.getRefs().get(ref.ref).ifPresent(s -> {
                final EnvKey env = new EnvKey(s, wf);
                initEnv(env);
                addBackRef(env, ref);
            });
        }

        private void addBackEdge(EnvKey srcEnv, IStep<S, L, O> st, EnvKey dstEnv) {
            backedges.__insert(srcEnv, Tuple2.of(dstEnv, st));
            final Collection<IDeclPath<S, L, O>> paths = envPaths.get(srcEnv).stream().flatMap(p -> {
                return Streams.stream(Paths.append(st, p));
            }).collect(CapsuleCollectors.toSet());
            worklist.push(new EnvTask(dstEnv, paths));
        }

        private void addBackImport(RefKey srcRef, L l, IRegExpMatcher<L> wf, EnvKey dstEnv) {
            backimports.__insert(srcRef, Tuple3.of(dstEnv, l, wf));
            final Collection<IDeclPath<S, L, O>> paths = refPaths.get(srcRef).stream().flatMap(p -> {
                return scopeGraph.getExportEdges().get(p.getDeclaration(), l).stream()
                        .flatMap(ss -> envPaths.get(new EnvKey(ss, wf)).stream()).flatMap(pp -> {
                            return Streams.stream(
                                    Paths.append(Paths.named(dstEnv.scope, l, p, pp.getPath().getSource()), pp));
                        });
            }).collect(CapsuleCollectors.toSet());
            worklist.push(new EnvTask(dstEnv, paths));
        }

        private void addBackRef(EnvKey srcEnv, RefKey dstRef) {
            backrefs.__insert(srcEnv, dstRef);
            final Collection<IResolutionPath<S, L, O>> paths = envPaths.get(srcEnv).stream().flatMap(p -> {
                return Streams.stream(Paths.resolve(dstRef.ref, p));
            }).collect(CapsuleCollectors.toSet());
            worklist.push(new RefTask(dstRef, paths));
        }

    }

    private <P extends IPath<S, L, O>> Collection<P> filter(Collection<P> paths, IRelation<L> order,
            Function3<P, P, IRelation<L>, Integer> compare) {
        final Set.Transient<P> filteredPaths = Set.Transient.of();
        NEXT: for(P path : paths) {
            for(P filteredPath : filteredPaths) {
                final Integer result = compare.apply(path, filteredPath, order);
                if(result != null) {
                    // paths are comparable
                    if(result < 0) {
                        // the candidate is smaller than an earlier selected path
                        filteredPaths.__remove(filteredPath);
                    }
                    if(result > 0) {
                        // the candidate is larger than an earlier selected path
                        continue NEXT;
                    }
                }
            }
            // there are no smaller selected paths
            filteredPaths.__insert(path);
        }
        return filteredPaths.freeze();
    }

    private Integer compare(IResolutionPath<S, L, O> path1, IResolutionPath<S, L, O> path2, IRelation<L> order) {
        if(!path1.getDeclaration().getSpacedName().equals(path2.getDeclaration().getSpacedName())) {
            return 0;
        }
        return compare(path1.getPath(), path2.getPath(), order);
    }

    private Integer compare(IDeclPath<S, L, O> path1, IDeclPath<S, L, O> path2, IRelation<L> order) {
        if(!path1.getDeclaration().getSpacedName().equals(path2.getDeclaration().getSpacedName())) {
            return 0;
        }
        return compare(path1.getPath(), path2.getPath(), order);
    }

    private Integer compare(IScopePath<S, L, O> path1, IScopePath<S, L, O> path2, IRelation<L> order) {
        Integer result = 0;
        final Iterator<IStep<S, L, O>> it1 = path1.iterator();
        final Iterator<IStep<S, L, O>> it2 = path2.iterator();
        while(it1.hasNext() && it2.hasNext()) {
            result = compare(it1.next(), it2.next(), order);
            if(result == null || result != 0) {
                return result;
            }
        }
        if(it1.hasNext()) {
            return order.contains(it1.next().getLabel(), labelD) ? -1 : 0;
        } else if(it2.hasNext()) {
            return order.contains(labelD, it2.next().getLabel()) ? 0 : 1;
        } else {
            return result;
        }
    }

    private Integer compare(IStep<S, L, O> step1, IStep<S, L, O> step2, IRelation<L> order) {
        if(!step1.getSource().equals(step2.getSource())) {
            // steps with different sources are incomparable
            return null;
        }
        if(!step1.getLabel().equals(step2.getLabel())) {
            // compare by labels
            if(order.contains(step1.getLabel(), step2.getLabel())) {
                // 1 < 2
                return -1;
            } else if(order.contains(step2.getLabel(), step1.getLabel())) {
                // 2 < 1
                return 1;
            } else {
                // steps are incomparable
                return null;
            }
        }
        if(!step1.getTarget().equals(step2.getTarget())) {
            // steps with same labels but different targets are incomparable
            return null;
        }
        // at this point, the two steps have the same source, target, and label
        final IResolutionPath<S, L, O> path1 = importPath(step1);
        final IResolutionPath<S, L, O> path2 = importPath(step2);
        if(path1 == null && path2 == null) {
            // the steps are equal
            return 0;
        } else if(path1 != null && path2 != null) {
            // both steps are imports, compare import paths
            return compare(path1, path2, this.order /* import paths are always ordered by visibility */);
        } else {
            // steps are incomparable
            return null;
        }
    }

    private IResolutionPath<S, L, O> importPath(IStep<S, L, O> step) {
        return step.match(new IStep.ICases<S, L, O, IResolutionPath<S, L, O>>() {
            @Override public IResolutionPath<S, L, O> caseE(S source, L label, S target) {
                return null;
            }

            @Override public IResolutionPath<S, L, O> caseN(S source, L label, IResolutionPath<S, L, O> importPath,
                    S target) {
                return importPath;
            }
        });
    }

    private class RefKey implements Serializable {
        private static final long serialVersionUID = 1L;

        public final O ref;

        public RefKey(O ref) {
            this.ref = ref;
        }

        @Override public int hashCode() {
            return Objects.hash(ref);
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") RefKey other = (RefKey) obj;
            return ref.equals(other.ref);
        }

        @Override public String toString() {
            return ref.toString();
        }

    }

    private class EnvKey implements Serializable {
        private static final long serialVersionUID = 1L;

        public final S scope;
        public final IRegExpMatcher<L> wf;

        public EnvKey(S scope, IRegExpMatcher<L> wf) {
            this.scope = scope;
            this.wf = wf;
        }

        @Override public int hashCode() {
            return Objects.hash(scope, wf.regexp());
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") EnvKey other = (EnvKey) obj;
            return scope.equals(other.scope) && wf.regexp().equals(other.wf.regexp());
        }

        @Override public String toString() {
            return scope.toString() + "/" + wf.regexp().toString();
        }

    }

}