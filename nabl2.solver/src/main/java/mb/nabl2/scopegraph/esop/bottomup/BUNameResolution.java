package mb.nabl2.scopegraph.esop.bottomup;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Collection;
import java.util.Deque;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Queues;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
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
import mb.nabl2.util.collections.HashTrieRelation2;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation2;
import mb.nabl2.util.collections.IRelation3;

public class BUNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements INameResolution<S, L, O> {

    private static final ILogger logger = LoggerUtils.logger(BUNameResolution.class);

    private static enum Kind {
        VISIBLE, // top-level and import paths are ordered
        REACHABLE, // top-level paths are unordered, import paths are ordered
        IMPORT // top-level and import paths are unordered
    }

    private final IEsopScopeGraph<S, L, O, ?> scopeGraph;
    private final Iterable<L> labels;
    private final IRegExpMatcher<L> wf;
    private final ImmutableMap<Kind, BUComparator<S, L, O>> orders;

    public BUNameResolution(IEsopScopeGraph<S, L, O, ?> scopeGraph, Iterable<L> labels, L labelD, IRegExp<L> wf,
            IRelation<L> order) {
        this.scopeGraph = scopeGraph;
        this.labels = labels;
        this.wf = RegExpMatcher.create(wf);
        final IRelation<L> noOrder = Relation.Immutable.of(RelationDescription.STRICT_PARTIAL_ORDER);
        // @formatter:off
        this.orders = ImmutableMap.of(
            Kind.VISIBLE,   new BUComparator<>(labelD, order, order),
            Kind.REACHABLE, new BUComparator<>(labelD, noOrder, order),
            Kind.IMPORT,    new BUComparator<>(labelD, noOrder, noOrder)
        );
        // @formatter:on
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

    @Override public Collection<Map.Entry<O, Collection<IResolutionPath<S, L, O>>>> resolutionEntries() {
        return resolved.entrySet();
    }

    private Map.Transient<O, Collection<IResolutionPath<S, L, O>>> resolved = Map.Transient.of();
    private Map.Transient<S, Collection<IDeclPath<S, L, O>>> reachable = Map.Transient.of();
    private Map.Transient<S, Collection<IDeclPath<S, L, O>>> visible = Map.Transient.of();

    private Map.Transient<EnvKey, BUEnv<S, L, O, IDeclPath<S, L, O>>> envPaths = Map.Transient.of();
    private Map.Transient<RefKey, BUEnv<S, L, O, IResolutionPath<S, L, O>>> refPaths = Map.Transient.of();

    private IRelation3.Transient<EnvKey, IStep<S, L, O>, EnvKey> backedges = HashTrieRelation3.Transient.of();
    private IRelation3.Transient<RefKey, Tuple2<L, IRegExpMatcher<L>>, EnvKey> backimports =
            HashTrieRelation3.Transient.of();
    private IRelation2.Transient<EnvKey, RefKey> backrefs = HashTrieRelation2.Transient.of();

    private final Set.Transient<EnvKey> initedEnvs = Set.Transient.of();
    private final Set.Transient<RefKey> initedRes = Set.Transient.of();

    private Collection<IResolutionPath<S, L, O>> resolveEnv(O ref) throws InterruptedException {
        Collection<IResolutionPath<S, L, O>> paths = resolved.get(ref);
        if(paths == null) {
            final RefKey key = new RefKey(Kind.VISIBLE, ref);
            new Compute(key).call();
            resolved.__put(ref, paths = refPaths.get(key).pathSet());
        }
        return paths;
    }

    private Collection<IDeclPath<S, L, O>> visibleEnv(S scope) throws InterruptedException {
        Collection<IDeclPath<S, L, O>> paths = visible.get(scope);
        if(paths == null) {
            final EnvKey key = new EnvKey(Kind.VISIBLE, scope, wf);
            new Compute(key).call();
            visible.__put(scope, paths = envPaths.get(key).pathSet());
        }
        return paths;
    }

    private Collection<IDeclPath<S, L, O>> reachableEnv(S scope) throws InterruptedException {
        Collection<IDeclPath<S, L, O>> paths = reachable.get(scope);
        if(paths == null) {
            final EnvKey key = new EnvKey(Kind.REACHABLE, scope, wf);
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

        private void initEnv(EnvKey env) {
            if(initedEnvs.contains(env)) {
                return;
            }
            initedEnvs.__insert(env);
            envPaths.__put(env, new BUEnv<>(orders.get(env.kind)::compare));
            if(env.wf.isAccepting()) {
                final Set.Immutable<IDeclPath<S, L, O>> declPaths = scopeGraph.getDecls().inverse().get(env.scope)
                        .stream().map(d -> Paths.decl(Paths.<S, L, O>empty(env.scope), d))
                        .collect(CapsuleCollectors.toSet());
                addTask(new EnvTask(env, new BUChanges<>(declPaths, Set.Immutable.of())));
            }
            for(L l : labels) {
                IRegExpMatcher<L> wf2 = env.wf.match(l);
                if(wf2.isEmpty()) {
                    continue;
                }
                for(S scope : scopeGraph.getDirectEdges().get(env.scope, l)) {
                    final EnvKey srcEnv = new EnvKey(env.kind, scope, wf2);
                    initEnv(srcEnv);
                    addBackEdge(srcEnv, Paths.direct(env.scope, l, srcEnv.scope), env);
                }
                for(O ref : scopeGraph.getImportEdges().get(env.scope, l)) {
                    final RefKey srcRef = new RefKey(Kind.IMPORT, ref);
                    initRef(srcRef);
                    addBackImport(srcRef, l, wf2, env);
                }
            }
        }

        private void initRef(RefKey ref) {
            if(initedRes.contains(ref)) {
                return;
            }
            initedRes.__insert(ref);
            refPaths.__put(ref, new BUEnv<>(orders.get(ref.kind)::compare));
            scopeGraph.getRefs().get(ref.ref).ifPresent(s -> {
                final EnvKey env = new EnvKey(ref.kind, s, wf);
                initEnv(env);
                addBackRef(env, ref);
            });
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

            private final EnvKey env;
            private final BUChanges<S, L, O, IDeclPath<S, L, O>> changes;

            public EnvTask(EnvKey env, BUChanges<S, L, O, IDeclPath<S, L, O>> changes) {
                this.env = env;
                this.changes = changes;
            }

            @Override void call() throws InterruptedException {
                if(changes.isEmpty()) {
                    return;
                }
                initEnv(env);
                final BUChanges<S, L, O, IDeclPath<S, L, O>> newChanges = envPaths.get(env).apply(changes);
                for(RefKey ref : backrefs.get(env)) {
                    final BUChanges<S, L, O, IResolutionPath<S, L, O>> refChanges =
                            newChanges.flatMap(p -> ofOpt(Paths.resolve(ref.ref, p)));
                    addTask(new RefTask(ref, refChanges));
                }
                for(Entry<IStep<S, L, O>, EnvKey> dstEnv : backedges.get(env)) {
                    final BUChanges<S, L, O, IDeclPath<S, L, O>> envChanges =
                            newChanges.flatMap(p -> ofOpt(Paths.append(dstEnv.getKey(), p)));
                    addTask(new EnvTask(dstEnv.getValue(), envChanges));
                }
            }

        }

        private class RefTask extends Task {

            private final RefKey ref;
            private final BUChanges<S, L, O, IResolutionPath<S, L, O>> changes;

            public RefTask(RefKey ref, BUChanges<S, L, O, IResolutionPath<S, L, O>> changes) {
                this.ref = ref;
                this.changes = changes;
            }

            @Override void call() throws InterruptedException {
                initRef(ref);
                final BUChanges<S, L, O, IResolutionPath<S, L, O>> newChanges = refPaths.get(ref).apply(changes);
                for(Entry<Tuple2<L, IRegExpMatcher<L>>, EnvKey> entry : backimports.get(ref)) {
                    for(IResolutionPath<S, L, O> p : newChanges.added()) {
                        for(S s : scopeGraph.getExportEdges().get(p.getDeclaration(), entry.getKey()._1())) {
                            final EnvKey env = new EnvKey(ref.kind, s, entry.getKey()._2());
                            initEnv(env);
                            addBackEdge(env, Paths.named(entry.getValue().scope, entry.getKey()._1(), p, env.scope),
                                    entry.getValue());
                        }
                    }
                    for(IResolutionPath<S, L, O> p : newChanges.removed()) {
                        for(S s : scopeGraph.getExportEdges().get(p.getDeclaration(), entry.getKey()._1())) {
                            final EnvKey env = new EnvKey(ref.kind, s, entry.getKey()._2());
                            initEnv(env); // necessary?
                            removeBackEdge(env, Paths.named(entry.getValue().scope, entry.getKey()._1(), p, env.scope),
                                    entry.getValue());
                        }
                    }
                }
            }

        }

        private void addBackEdge(EnvKey srcEnv, IStep<S, L, O> st, EnvKey dstEnv) {
            if(!backedges.put(srcEnv, st, dstEnv)) {
                return;
            }
            final Set.Immutable<IDeclPath<S, L, O>> paths = envPaths.get(srcEnv).pathSet().stream().flatMap(p -> {
                return ofOpt(Paths.append(st, p));
            }).collect(CapsuleCollectors.toSet());
            addTask(new EnvTask(dstEnv, new BUChanges<>(paths, Set.Immutable.of())));
        }

        private void removeBackEdge(EnvKey srcEnv, IStep<S, L, O> st, EnvKey dstEnv) {
            if(!backedges.remove(srcEnv, st, dstEnv)) {
                return;
            }
            final Set.Immutable<IDeclPath<S, L, O>> paths = envPaths.get(srcEnv).pathSet().stream().flatMap(p -> {
                return ofOpt(Paths.append(st, p));
            }).collect(CapsuleCollectors.toSet());
            addTask(new EnvTask(dstEnv, new BUChanges<>(Set.Immutable.of(), paths)));
        }

        private void addBackImport(RefKey srcRef, L l, IRegExpMatcher<L> wf, EnvKey dstEnv) {
            if(!srcRef.kind.equals(Kind.IMPORT)) {
                throw new AssertionError();
            }
            backimports.put(srcRef, Tuple2.of(l, wf), dstEnv);
            refPaths.get(srcRef).pathSet().stream().forEach(p -> {
                scopeGraph.getExportEdges().get(p.getDeclaration(), l).forEach(ss -> {
                    final EnvKey srcEnv = new EnvKey(dstEnv.kind, ss, wf);
                    final IStep<S, L, O> st = Paths.named(dstEnv.scope, l, p, srcEnv.scope);
                    initEnv(srcEnv);
                    addBackEdge(srcEnv, st, dstEnv);
                });
            });
        }

        private void addBackRef(EnvKey srcEnv, RefKey dstRef) {
            backrefs.put(srcEnv, dstRef);
            final Set.Immutable<IResolutionPath<S, L, O>> paths = envPaths.get(srcEnv).pathSet().stream().flatMap(p -> {
                return ofOpt(Paths.resolve(dstRef.ref, p));
            }).collect(CapsuleCollectors.toSet());
            addTask(new RefTask(dstRef, new BUChanges<>(paths, Set.Immutable.of())));
        }

        private void addTask(Task t) {
            worklist.addLast(t);
        }

    }

    private class RefKey implements Serializable {
        private static final long serialVersionUID = 1L;

        public final Kind kind;
        public final O ref;

        public RefKey(Kind kind, O ref) {
            this.kind = kind;
            this.ref = ref;
        }

        @Override public int hashCode() {
            return Objects.hash(kind, ref);
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") RefKey other = (RefKey) obj;
            return kind.equals(other.kind) && ref.equals(other.ref);
        }

        @Override public String toString() {
            return kind.name() + "/" + ref.toString();
        }

    }

    private class EnvKey implements Serializable {
        private static final long serialVersionUID = 1L;

        public final Kind kind;
        public final S scope;
        public final IRegExpMatcher<L> wf;

        public EnvKey(Kind kind, S scope, IRegExpMatcher<L> wf) {
            this.kind = kind;
            this.scope = scope;
            this.wf = wf;
            if(wf.isEmpty()) {
                throw new AssertionError();
            }
        }

        @Override public int hashCode() {
            return Objects.hash(kind, scope, wf.regexp());
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") EnvKey other = (EnvKey) obj;
            return kind.equals(other.kind) && scope.equals(other.scope) && wf.regexp().equals(other.wf.regexp());
        }

        @Override public String toString() {
            return kind.toString() + "/" + scope.toString() + "/" + wf.regexp().toString();
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
        for(Tuple3<EnvKey, IStep<S, L, O>, EnvKey> backedge : (Iterable<Tuple3<EnvKey, IStep<S, L, O>, EnvKey>>) backedges
                .stream()::iterator) {
            out.write(prefix + "| - " + backedge._3() + " -" + backedge._2() + "-< " + backedge._1() + "\n");
        }
        out.write(prefix + "| back imports:\n");
        for(Tuple3<RefKey, Tuple2<L, IRegExpMatcher<L>>, EnvKey> backimport : (Iterable<Tuple3<RefKey, Tuple2<L, IRegExpMatcher<L>>, EnvKey>>) backimports
                .stream()::iterator) {
            out.write(prefix + "| - " + backimport._3() + " =" + backimport._2()._1() + "=< " + backimport._1() + "\n");
        }
        out.write(prefix + "| ref paths:\n");
        for(RefKey ref : initedRes) {
            final BUEnv<S, L, O, IResolutionPath<S, L, O>> paths = refPaths.get(ref);
            out.write(prefix + "| - " + ref + ":\n");
            paths.write(prefix + "|   | ", out);
        }
        out.write("| env paths:\n");
        for(EnvKey env : initedEnvs) {
            final BUEnv<S, L, O, IDeclPath<S, L, O>> paths = envPaths.get(env);
            out.write(prefix + "| - " + env + ":\n");
            paths.write(prefix + "|   | ", out);
        }
    }

    private static <X> Stream<X> ofOpt(Optional<X> xOrNull) {
        return Streams.stream(xOrNull);
    }

}