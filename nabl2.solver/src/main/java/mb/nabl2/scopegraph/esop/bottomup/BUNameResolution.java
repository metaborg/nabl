package mb.nabl2.scopegraph.esop.bottomup;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Collection;
import java.util.Deque;
import java.util.Map.Entry;
import java.util.Objects;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

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
import mb.nabl2.scopegraph.path.IResolutionPath;
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

    private static final ILogger logger = LoggerUtils.logger(BUNameResolution.class);

    private final IEsopScopeGraph<S, L, O, ?> scopeGraph;
    private final Iterable<L> labels;
    private final IRegExpMatcher<L> wf;
    private final BUComparator<S, L, O> reachableOrder;
    private final BUComparator<S, L, O> visibleOrder;

    public BUNameResolution(IEsopScopeGraph<S, L, O, ?> scopeGraph, Iterable<L> labels, L labelD, IRegExp<L> wf,
            IRelation<L> order) {
        this.scopeGraph = scopeGraph;
        this.labels = labels;
        this.wf = RegExpMatcher.create(wf);
        final IRelation<L> noOrder = Relation.Immutable.of(RelationDescription.STRICT_PARTIAL_ORDER);
        this.reachableOrder = new BUComparator<>(labelD, noOrder, order);
        this.visibleOrder = new BUComparator<>(labelD, order, order);
    }

    public static BUNameResolution<Scope, Label, Occurrence> of(IEsopScopeGraph<Scope, Label, Occurrence, ?> scopeGraph,
            ResolutionParameters params) {
        return new BUNameResolution<>(scopeGraph, params.getLabels(), params.getLabelD(), params.getPathWf(),
                params.getSpecificityOrder());
    }

    @Override public java.util.Set<O> getResolvedRefs() {
        return resolved.keySet();
    }

    @Override public Collection<IResolutionPath<S, L, O>> resolve(O ref) throws InterruptedException {
        return resolveEnv(ref);
    }

    @Override public Collection<O> decls(S scope) {
        return scopeGraph.getDecls().inverse().get(scope);
    }

    @Override public Collection<O> refs(S scope) {
        return scopeGraph.getRefs().inverse().get(scope);
    }

    @Override public Collection<O> visible(S scope) throws InterruptedException {
        return Paths.declPathsToDecls(visibleEnv(scope));
    }

    @Override public Collection<O> reachable(S scope) throws InterruptedException {
        return Paths.declPathsToDecls(reachableEnv(scope));
    }

    @Override public Collection<? extends Map.Entry<O, ? extends Collection<IResolutionPath<S, L, O>>>>
            resolutionEntries() {
        return resolved.entrySet();
    }

    private Map.Transient<O, Collection<IResolutionPath<S, L, O>>> resolved = Map.Transient.of();
    private Map.Transient<S, Collection<IDeclPath<S, L, O>>> reachable = Map.Transient.of();
    private Map.Transient<S, Collection<IDeclPath<S, L, O>>> visible = Map.Transient.of();

    private Map.Transient<EnvKey, BUEnv<S, L, O, IDeclPath<S, L, O>>> envPaths = Map.Transient.of();
    private Map.Transient<RefKey, BUEnv<S, L, O, IResolutionPath<S, L, O>>> refPaths = Map.Transient.of();

    private SetMultimap.Transient<EnvKey, Tuple2<EnvKey, IStep<S, L, O>>> backedges = SetMultimap.Transient.of();
    private SetMultimap.Transient<RefKey, Tuple3<EnvKey, L, IRegExpMatcher<L>>> backimports =
            SetMultimap.Transient.of();
    private SetMultimap.Transient<EnvKey, RefKey> backrefs = SetMultimap.Transient.of();

    private final Set.Transient<EnvKey> initedEnvs = Set.Transient.of();
    private final Set.Transient<RefKey> initedRes = Set.Transient.of();

    private Collection<IResolutionPath<S, L, O>> resolveEnv(O ref) throws InterruptedException {
        Collection<IResolutionPath<S, L, O>> paths = resolved.get(ref);
        if(paths == null) {
            final RefKey key = new RefKey(ref);
            new Compute(key).call();
            resolved.__put(ref, paths = refPaths.get(key).pathSet());
        }
        return paths;
    }

    private Collection<IDeclPath<S, L, O>> visibleEnv(S scope) throws InterruptedException {
        Collection<IDeclPath<S, L, O>> paths = visible.get(scope);
        if(paths == null) {
            final EnvKey key = new EnvKey(scope, wf);
            new Compute(key).call();
            BUEnv<S, L, O, IDeclPath<S, L, O>> env = new BUEnv<>(visibleOrder::compare);
            env.addAll(envPaths.get(key).pathSet());
            visible.__put(scope, paths = env.pathSet());
        }
        return paths;
    }

    private Collection<IDeclPath<S, L, O>> reachableEnv(S scope) throws InterruptedException {
        Collection<IDeclPath<S, L, O>> paths = reachable.get(scope);
        if(paths == null) {
            final EnvKey key = new EnvKey(scope, wf);
            new Compute(key).call();
            reachable.__put(scope, paths = envPaths.get(key).pathSet());
        }
        return paths;
    }

    private class Compute {

        private final Deque<Task> worklist = Queues.newArrayDeque();

        public Compute(RefKey ref) {
            initRef(ref);
        }

        public Compute(EnvKey env) {
            initEnv(env);
        }

        public void call() throws InterruptedException {

            while(!worklist.isEmpty()) {
                worklist.pop().call();
            }

        }

        abstract class Task {

            abstract void call() throws InterruptedException;

        }

        private class EnvTask extends Task {

            public final EnvKey env;
            public final Set.Immutable<IDeclPath<S, L, O>> newPaths;

            public EnvTask(EnvKey env, Set.Immutable<IDeclPath<S, L, O>> paths) {
                this.env = env;
                this.newPaths = paths;
            }

            @Override void call() throws InterruptedException {
                if(newPaths.isEmpty()) {
                    return;
                }
                initEnv(env);
                final Set.Immutable<IDeclPath<S, L, O>> addedPaths = envPaths.get(env).addAll(newPaths);
                for(RefKey ref : backrefs.get(env)) {
                    final Set.Immutable<IResolutionPath<S, L, O>> refPaths = addedPaths.stream()
                            .flatMap(p -> Streams.stream(Paths.resolve(ref.ref, p))).collect(CapsuleCollectors.toSet());
                    worklist.push(new RefTask(ref, refPaths));
                }
                for(Tuple2<EnvKey, IStep<S, L, O>> env2 : backedges.get(env)) {
                    final Set.Immutable<IDeclPath<S, L, O>> env2Paths =
                            addedPaths.stream().flatMap(p -> Streams.stream(Paths.append(env2._2(), p)))
                                    .collect(CapsuleCollectors.toSet());
                    worklist.push(new EnvTask(env2._1(), env2Paths));
                }
            }

        }

        private class RefTask extends Task {

            private final RefKey ref;
            private final Set.Immutable<IResolutionPath<S, L, O>> newPaths;

            public RefTask(RefKey ref, Set.Immutable<IResolutionPath<S, L, O>> paths) {
                this.ref = ref;
                this.newPaths = paths;
            }

            @Override void call() throws InterruptedException {
                initRef(ref);
                final Set.Immutable<IResolutionPath<S, L, O>> addedPaths = refPaths.get(ref).addAll(newPaths);
                for(Tuple3<EnvKey, L, IRegExpMatcher<L>> entry : backimports.get(ref)) {
                    for(IResolutionPath<S, L, O> p : addedPaths) {
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
            envPaths.__put(env, new BUEnv<>(reachableOrder::compare));
            if(env.wf.isAccepting()) {
                final Set.Immutable<IDeclPath<S, L, O>> declPaths = scopeGraph.getDecls().inverse().get(env.scope)
                        .stream().map(d -> Paths.decl(Paths.<S, L, O>empty(env.scope), d))
                        .collect(CapsuleCollectors.toSet());
                worklist.push(new EnvTask(env, declPaths));
            }
            for(L l : labels) {
                IRegExpMatcher<L> wf2 = env.wf.match(l);
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
            if(initedRes.contains(ref)) {
                return;
            }
            initedRes.__insert(ref);
            refPaths.__put(ref, new BUEnv<>(visibleOrder::compare));
            scopeGraph.getRefs().get(ref.ref).ifPresent(s -> {
                final EnvKey env = new EnvKey(s, wf);
                initEnv(env);
                addBackRef(env, ref);
            });
        }

        private void addBackEdge(EnvKey srcEnv, IStep<S, L, O> st, EnvKey dstEnv) {
            backedges.__insert(srcEnv, Tuple2.of(dstEnv, st));
            final Set.Immutable<IDeclPath<S, L, O>> paths = envPaths.get(srcEnv).pathSet().stream().flatMap(p -> {
                return Streams.stream(Paths.append(st, p));
            }).collect(CapsuleCollectors.toSet());
            worklist.push(new EnvTask(dstEnv, paths));
        }

        private void addBackImport(RefKey srcRef, L l, IRegExpMatcher<L> wf, EnvKey dstEnv) {
            backimports.__insert(srcRef, Tuple3.of(dstEnv, l, wf));
            final Set.Immutable<IDeclPath<S, L, O>> paths = refPaths.get(srcRef).pathSet().stream().flatMap(p -> {
                return scopeGraph.getExportEdges().get(p.getDeclaration(), l).stream().flatMap(ss -> {
                    final EnvKey env2 = new EnvKey(ss, wf);
                    initEnv(env2);
                    return envPaths.get(env2).pathSet().stream();
                }).flatMap(pp -> {
                    return Streams.stream(Paths.append(Paths.named(dstEnv.scope, l, p, pp.getPath().getSource()), pp));
                });
            }).collect(CapsuleCollectors.toSet());
            worklist.push(new EnvTask(dstEnv, paths));
        }

        private void addBackRef(EnvKey srcEnv, RefKey dstRef) {
            backrefs.__insert(srcEnv, dstRef);
            final Set.Immutable<IResolutionPath<S, L, O>> paths = envPaths.get(srcEnv).pathSet().stream().flatMap(p -> {
                return Streams.stream(Paths.resolve(dstRef.ref, p));
            }).collect(CapsuleCollectors.toSet());
            worklist.push(new RefTask(dstRef, paths));
        }

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
            if(wf.isEmpty()) {
                throw new AssertionError();
            }
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

    public void write(Writer out) throws IOException {
        write("", out);
    }

    public void write(String prefix, Writer out) throws IOException {
        out.write(prefix + "bu:\n");
        out.write(prefix + "| back refs:\n");
        for(Entry<EnvKey, RefKey> backref : backrefs.entrySet()) {
            out.write(prefix + "| - " + backref.getValue() + " -< " + backref.getKey() + "\n");
        }
        out.write(prefix + "| back edges:\n");
        for(Entry<EnvKey, Tuple2<EnvKey, IStep<S, L, O>>> backedge : backedges.entrySet()) {
            out.write(prefix + "| - " + backedge.getValue()._1() + " -" + backedge.getValue()._2() + "-< "
                    + backedge.getKey() + "\n");
        }
        out.write(prefix + "| back imports:\n");
        for(Entry<RefKey, Tuple3<EnvKey, L, IRegExpMatcher<L>>> backimport : backimports.entrySet()) {
            out.write(prefix + "| - " + backimport.getValue()._1() + " =" + backimport.getValue()._2() + "=< "
                    + backimport.getKey() + "\n");
        }
        out.write(prefix + "| ref paths:\n");
        for(Entry<RefKey, BUEnv<S, L, O, IResolutionPath<S, L, O>>> refPath : refPaths.entrySet()) {
            out.write(prefix + "| - " + refPath.getKey() + ":\n");
            refPath.getValue().write(prefix + "|   | ", out);
        }
        out.write("| env paths:\n");
        for(Entry<EnvKey, BUEnv<S, L, O, IDeclPath<S, L, O>>> envPath : envPaths.entrySet()) {
            out.write(prefix + "| - " + envPath.getKey() + ":\n");
            envPath.getValue().write(prefix + "|   | ", out);
        }
    }

}