package org.metaborg.meta.nabl2.scopegraph.esop.reference;

import java.io.Serializable;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.util.collections.HashTrieFunction;
import org.metaborg.meta.nabl2.util.collections.HashTrieRelation3;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.functions.Predicate3;

import io.usethesource.capsule.Set;

public abstract class EsopScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence, V>
        implements IEsopScopeGraph<S, L, O, V> {

    protected EsopScopeGraph() {
    }

    @Override public Set.Immutable<S> getAllScopes() {
        Set.Transient<S> allScopes = Set.Transient.of();
        allScopes.__insertAll(getDecls().valueSet());
        allScopes.__insertAll(getRefs().valueSet());
        allScopes.__insertAll(getDirectEdges().keySet());
        allScopes.__insertAll(getDirectEdges().valueSet());
        allScopes.__insertAll(getExportEdges().valueSet());
        allScopes.__insertAll(getImportEdges().keySet());
        allScopes.__insertAll(incompleteImportEdges().keySet());
        allScopes.__insertAll(incompleteImportEdges().keySet());
        return allScopes.freeze();
    }

    @Override public Set.Immutable<O> getAllDecls() {
        Set.Transient<O> allDecls = Set.Transient.of();
        allDecls.__insertAll(getDecls().keySet());
        allDecls.__insertAll(getExportEdges().keySet());
        return allDecls.freeze();
    }

    @Override public Set.Immutable<O> getAllRefs() {
        Set.Transient<O> allRefs = Set.Transient.of();
        allRefs.__insertAll(getRefs().keySet());
        allRefs.__insertAll(getImportEdges().valueSet());
        return allRefs.freeze();
    }

    @Override public boolean isComplete() {
        return incompleteDirectEdges().isEmpty() && incompleteImportEdges().isEmpty();
    }

    @Override public boolean isOpen(S scope, L label) {
        return incompleteDirectEdges().contains(scope, label) || incompleteImportEdges().contains(scope, label);
    }

    // ------------------------------------

    public static class Immutable<S extends IScope, L extends ILabel, O extends IOccurrence, V>
            extends EsopScopeGraph<S, L, O, V> implements IEsopScopeGraph.Immutable<S, L, O, V>, Serializable {
        private static final long serialVersionUID = 42L;

        private final IFunction.Immutable<O, S> decls;
        private final IFunction.Immutable<O, S> refs;
        private final IRelation3.Immutable<S, L, S> directEdges;
        private final IRelation3.Immutable<O, L, S> assocEdges;
        private final IRelation3.Immutable<S, L, O> importEdges;

        private final IRelation3.Immutable<S, L, V> incompleteDirectEdges;
        private final IRelation3.Immutable<S, L, V> incompleteImportEdges;

        Immutable(IFunction.Immutable<O, S> decls, IFunction.Immutable<O, S> refs,
                IRelation3.Immutable<S, L, S> directEdges, IRelation3.Immutable<O, L, S> assocEdges,
                IRelation3.Immutable<S, L, O> importEdges, IRelation3.Immutable<S, L, V> incompleteDirectEdges,
                IRelation3.Immutable<S, L, V> incompleteImportEdges) {
            this.decls = decls;
            this.refs = refs;
            this.directEdges = directEdges;
            this.assocEdges = assocEdges;
            this.importEdges = importEdges;
            this.incompleteDirectEdges = incompleteDirectEdges;
            this.incompleteImportEdges = incompleteImportEdges;
        }

        // ------------------------------------------------------------

        @Override public IFunction.Immutable<O, S> getDecls() {
            return decls;
        }

        @Override public IFunction.Immutable<O, S> getRefs() {
            return refs;
        }

        @Override public IRelation3.Immutable<S, L, S> getDirectEdges() {
            return directEdges;
        }

        @Override public IRelation3.Immutable<S, L, V> incompleteDirectEdges() {
            return incompleteDirectEdges;
        }

        @Override public IRelation3.Immutable<O, L, S> getExportEdges() {
            return assocEdges;
        }

        @Override public IRelation3.Immutable<S, L, O> getImportEdges() {
            return importEdges;
        }

        @Override public IRelation3.Immutable<S, L, V> incompleteImportEdges() {
            return incompleteImportEdges;
        }

        // ------------------------------------------------------------

        public EsopScopeGraph.Transient<S, L, O, V> melt() {
            return new EsopScopeGraph.Transient<>(decls.melt(), refs.melt(), directEdges.melt(), assocEdges.melt(),
                    importEdges.melt(), incompleteDirectEdges.melt(), incompleteImportEdges.melt());
        }

        public static <S extends IScope, L extends ILabel, O extends IOccurrence, V>
                EsopScopeGraph.Immutable<S, L, O, V> of() {
            return new EsopScopeGraph.Immutable<>(HashTrieFunction.Immutable.of(), HashTrieFunction.Immutable.of(),
                    HashTrieRelation3.Immutable.of(), HashTrieRelation3.Immutable.of(),
                    HashTrieRelation3.Immutable.of(), HashTrieRelation3.Immutable.of(),
                    HashTrieRelation3.Immutable.of());
        }

    }


    public static class Transient<S extends IScope, L extends ILabel, O extends IOccurrence, V>
            extends EsopScopeGraph<S, L, O, V> implements IEsopScopeGraph.Transient<S, L, O, V> {

        private final IFunction.Transient<O, S> decls;
        private final IFunction.Transient<O, S> refs;
        private final IRelation3.Transient<S, L, S> directEdges;
        private final IRelation3.Transient<O, L, S> assocEdges;
        private final IRelation3.Transient<S, L, O> importEdges;

        private final IRelation3.Transient<S, L, V> incompleteDirectEdges;
        private final IRelation3.Transient<S, L, V> incompleteImportEdges;

        Transient(IFunction.Transient<O, S> decls, IFunction.Transient<O, S> refs,
                IRelation3.Transient<S, L, S> directEdges, IRelation3.Transient<O, L, S> assocEdges,
                IRelation3.Transient<S, L, O> importEdges, IRelation3.Transient<S, L, V> incompleteDirectEdges,
                IRelation3.Transient<S, L, V> incompleteImportEdges) {
            this.decls = decls;
            this.refs = refs;
            this.directEdges = directEdges;
            this.assocEdges = assocEdges;
            this.importEdges = importEdges;
            this.incompleteDirectEdges = incompleteDirectEdges;
            this.incompleteImportEdges = incompleteImportEdges;
        }

        // ------------------------------------------------------------

        @Override public IFunction<O, S> getDecls() {
            return decls;
        }

        @Override public IFunction<O, S> getRefs() {
            return refs;
        }

        @Override public IRelation3<S, L, S> getDirectEdges() {
            return directEdges;
        }

        @Override public IRelation3<S, L, V> incompleteDirectEdges() {
            return incompleteDirectEdges;
        }

        @Override public IRelation3<O, L, S> getExportEdges() {
            return assocEdges;
        }

        @Override public IRelation3<S, L, O> getImportEdges() {
            return importEdges;
        }

        @Override public IRelation3<S, L, V> incompleteImportEdges() {
            return incompleteImportEdges;
        }

        // ------------------------------------------------------------

        @Override public boolean addDecl(S scope, O decl) {
            return decls.put(decl, scope);
        }

        @Override public boolean addRef(O ref, S scope) {
            return refs.put(ref, scope);
        }

        @Override public boolean addDirectEdge(S sourceScope, L label, S targetScope) {
            return directEdges.put(sourceScope, label, targetScope);
        }

        @Override public boolean addIncompleteDirectEdge(S scope, L label, V var) {
            return incompleteDirectEdges.put(scope, label, var);
        }

        @Override public boolean addExportEdge(O decl, L label, S scope) {
            return assocEdges.put(decl, label, scope);
        }

        @Override public boolean addIncompleteImportEdge(S scope, L label, V var) {
            return incompleteImportEdges.put(scope, label, var);
        }

        @Override public boolean addImportEdge(S scope, L label, O ref) {
            return importEdges.put(scope, label, ref);
        }

        @Override public boolean addAll(IEsopScopeGraph<S, L, O, V> other) {
            boolean change = false;
            change |= decls.putAll(other.getDecls());
            change |= refs.putAll(other.getRefs());
            change |= directEdges.putAll(other.getDirectEdges());
            change |= assocEdges.putAll(other.getExportEdges());
            change |= importEdges.putAll(other.getImportEdges());
            change |= incompleteDirectEdges.putAll(other.incompleteDirectEdges());
            change |= incompleteImportEdges.putAll(other.incompleteImportEdges());
            return change;
        }

        // -------------------------

        public boolean reduce(PartialFunction1<V, S> fs, PartialFunction1<V, O> fo) {
            boolean progress = false;
            progress |= reduce(incompleteDirectEdges, fs, this::addDirectEdge);
            progress |= reduce(incompleteImportEdges, fo, this::addImportEdge);
            return progress;
        }

        private <X> boolean reduce(IRelation3.Transient<S, L, V> relation, PartialFunction1<V, X> f,
                Predicate3<S, L, X> add) {
            return relation.stream().flatMap(slv -> {
                return f.apply(slv._3()).map(x -> {
                    add.test(slv._1(), slv._2(), x);
                    return Stream.of(slv);
                }).orElse(Stream.empty());
            }).map(slv -> {
                return relation.remove(slv._1(), slv._2(), slv._3());
            }).findAny().isPresent();
        }

        // ------------------------------------------------------------

        public EsopScopeGraph.Immutable<S, L, O, V> freeze() {
            return new EsopScopeGraph.Immutable<>(decls.freeze(), refs.freeze(), directEdges.freeze(),
                    assocEdges.freeze(), importEdges.freeze(), incompleteDirectEdges.freeze(),
                    incompleteImportEdges.freeze());
        }

        public static <S extends IScope, L extends ILabel, O extends IOccurrence, V>
                EsopScopeGraph.Transient<S, L, O, V> of() {
            return new EsopScopeGraph.Transient<>(HashTrieFunction.Transient.of(), HashTrieFunction.Transient.of(),
                    HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                    HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                    HashTrieRelation3.Transient.of());
        }

    }


    public static <S extends IScope, L extends ILabel, O extends IOccurrence, V> Extension<S, L, O, V>
            extend(IEsopScopeGraph.Transient<S, L, O, V> graph1, IEsopScopeGraph<S, L, O, V> graph2) {
        return new Extension<>(graph1, graph2);
    }

    public static class Extension<S extends IScope, L extends ILabel, O extends IOccurrence, V>
            extends EsopScopeGraph<S, L, O, V> implements IEsopScopeGraph.Transient<S, L, O, V> {

        private final IEsopScopeGraph.Transient<S, L, O, V> graph1;
        private final IEsopScopeGraph<S, L, O, V> graph2;

        private Extension(IEsopScopeGraph.Transient<S, L, O, V> graph1, IEsopScopeGraph<S, L, O, V> graph2) {
            this.graph1 = graph1;
            this.graph2 = graph2;
        }

        public IRelation3<S, L, V> incompleteDirectEdges() {
            return HashTrieRelation3.union(graph1.incompleteDirectEdges(), graph2.incompleteDirectEdges());
        }

        public IRelation3<S, L, V> incompleteImportEdges() {
            return HashTrieRelation3.union(graph1.incompleteImportEdges(), graph2.incompleteImportEdges());
        }

        public IFunction<O, S> getDecls() {
            return HashTrieFunction.union(graph1.getDecls(), graph2.getDecls());
        }

        public IFunction<O, S> getRefs() {
            return HashTrieFunction.union(graph1.getRefs(), graph2.getRefs());
        }

        public IRelation3<S, L, S> getDirectEdges() {
            return HashTrieRelation3.union(graph1.getDirectEdges(), graph2.getDirectEdges());
        }

        public IRelation3<O, L, S> getExportEdges() {
            return HashTrieRelation3.union(graph1.getExportEdges(), graph2.getExportEdges());
        }

        public IRelation3<S, L, O> getImportEdges() {
            return HashTrieRelation3.union(graph1.getImportEdges(), graph2.getImportEdges());
        }

        public boolean addDecl(S scope, O decl) {
            return graph1.addDecl(scope, decl);
        }

        public boolean addRef(O ref, S scope) {
            return graph1.addRef(ref, scope);
        }

        public boolean addDirectEdge(S sourceScope, L label, S targetScope) {
            return graph1.addDirectEdge(sourceScope, label, targetScope);
        }

        public boolean addIncompleteDirectEdge(S scope, L label, V var) {
            return graph1.addIncompleteDirectEdge(scope, label, var);
        }

        public boolean addExportEdge(O decl, L label, S scope) {
            return graph1.addExportEdge(decl, label, scope);
        }

        public boolean addImportEdge(S scope, L label, O ref) {
            return graph1.addImportEdge(scope, label, ref);
        }

        public boolean addIncompleteImportEdge(S scope, L label, V var) {
            return graph1.addIncompleteImportEdge(scope, label, var);
        }

        public boolean addAll(IEsopScopeGraph<S, L, O, V> other) {
            return graph1.addAll(other);
        }

        public boolean reduce(PartialFunction1<V, S> fs, PartialFunction1<V, O> fo) {
            return graph1.reduce(fs, fo);
        }

        public IEsopScopeGraph.Immutable<S, L, O, V> freeze() {
            return graph1.freeze();
        }

    }

}