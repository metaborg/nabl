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
import org.metaborg.meta.nabl2.util.functions.PartialFunction1;
import org.metaborg.meta.nabl2.util.functions.Predicate3;

import io.usethesource.capsule.Set;

public abstract class EsopScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence, V>
        implements IEsopScopeGraph<S, L, O, V> {

    private final IFunction<O, S> decls;
    private final IFunction<O, S> refs;
    private final IRelation3<S, L, S> directEdges;
    private final IRelation3<O, L, S> assocEdges;
    private final IRelation3<S, L, O> importEdges;

    private final IRelation3<S, L, V> incompleteDirectEdges;
    private final IRelation3<S, L, V> incompleteImportEdges;

    private EsopScopeGraph(IFunction<O, S> decls, IFunction<O, S> refs, IRelation3<S, L, S> directEdges,
            IRelation3<O, L, S> assocEdges, IRelation3<S, L, O> importEdges, IRelation3<S, L, V> incompleteDirectEdges,
            IRelation3<S, L, V> incompleteImportEdges) {
        this.decls = decls;
        this.refs = refs;
        this.directEdges = directEdges;
        this.assocEdges = assocEdges;
        this.importEdges = importEdges;
        this.incompleteDirectEdges = incompleteDirectEdges;
        this.incompleteImportEdges = incompleteImportEdges;
    }

    // ------------------------------------------------------------

    @Override public Set.Immutable<S> getAllScopes() {
        Set.Transient<S> allScopes = Set.Transient.of();
        allScopes.__insertAll(decls.valueSet());
        allScopes.__insertAll(refs.valueSet());
        allScopes.__insertAll(directEdges.keySet());
        allScopes.__insertAll(directEdges.valueSet());
        allScopes.__insertAll(assocEdges.valueSet());
        allScopes.__insertAll(importEdges.keySet());
        allScopes.__insertAll(incompleteImportEdges.keySet());
        allScopes.__insertAll(incompleteImportEdges.keySet());
        return allScopes.freeze();
    }

    @Override public Set.Immutable<O> getAllDecls() {
        Set.Transient<O> allDecls = Set.Transient.of();
        allDecls.__insertAll(decls.keySet());
        allDecls.__insertAll(assocEdges.keySet());
        return allDecls.freeze();
    }

    @Override public Set.Immutable<O> getAllRefs() {
        Set.Transient<O> allRefs = Set.Transient.of();
        allRefs.__insertAll(refs.keySet());
        allRefs.__insertAll(importEdges.valueSet());
        return allRefs.freeze();
    }

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

    @Override public boolean isComplete() {
        return incompleteDirectEdges.isEmpty() && incompleteImportEdges.isEmpty();
    }

    @Override public boolean isOpen(S scope, L label) {
        return incompleteDirectEdges.contains(scope, label) || incompleteImportEdges.contains(scope, label);
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
            super(decls, refs, directEdges, assocEdges, importEdges, incompleteDirectEdges, incompleteImportEdges);
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
            super(decls, refs, directEdges, assocEdges, importEdges, incompleteDirectEdges, incompleteImportEdges);
            this.decls = decls;
            this.refs = refs;
            this.directEdges = directEdges;
            this.assocEdges = assocEdges;
            this.importEdges = importEdges;
            this.incompleteDirectEdges = incompleteDirectEdges;
            this.incompleteImportEdges = incompleteImportEdges;
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

        @Override public boolean addAssoc(O decl, L label, S scope) {
            return assocEdges.put(decl, label, scope);
        }

        @Override public boolean addIncompleteImportEdge(S scope, L label, V var) {
            return incompleteImportEdges.put(scope, label, var);
        }

        @Override public boolean addImport(S scope, L label, O ref) {
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
            progress |= reduce(incompleteImportEdges, fo, this::addImport);
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

}