package org.metaborg.meta.nabl2.scopegraph.esop.reference;

import java.io.Serializable;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.util.collections.HashTrieFunction;
import org.metaborg.meta.nabl2.util.collections.HashTrieRelation3;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.functions.PartialFunction1;
import org.metaborg.meta.nabl2.util.functions.Predicate2;
import org.metaborg.meta.nabl2.util.functions.Predicate3;

import io.usethesource.capsule.Set;

public abstract class EsopScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence, V>
        implements IEsopScopeGraph<S, L, O, V> {

    private final Set<S> allScopes;
    private final Set<O> allDecls;
    private final Set<O> allRefs;

    private final IFunction<O, S> decls;
    private final IFunction<O, S> refs;
    private final IRelation3<S, L, S> directEdges;
    private final IRelation3<O, L, S> assocEdges;
    private final IRelation3<S, L, O> importEdges;

    private final IRelation3<S, L, V> incompleteDirectEdges;
    private final IRelation3<S, L, V> incompleteImportEdges;

    private EsopScopeGraph(Set<S> allScopes, Set<O> allDecls, Set<O> allRefs, IFunction<O, S> decls,
            IFunction<O, S> refs, IRelation3<S, L, S> directEdges, IRelation3<O, L, S> assocEdges,
            IRelation3<S, L, O> importEdges, IRelation3<S, L, V> incompleteDirectEdges,
            IRelation3<S, L, V> incompleteImportEdges) {
        this.allScopes = allScopes;
        this.allDecls = allDecls;
        this.allRefs = allRefs;
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
        return Set.Immutable.<S>of().__insertAll(allScopes);
    }

    @Override public Set.Immutable<O> getAllDecls() {
        return Set.Immutable.<O>of().__insertAll(allDecls);
    }

    @Override public Set.Immutable<O> getAllRefs() {
        return Set.Immutable.<O>of().__insertAll(allRefs);
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

    @Override public EsopNameResolution<S, L, O> resolve(IResolutionParameters<L> params,
            Predicate2<S, L> isEdgeClosed) {
        return new EsopNameResolution<>(this, params, isEdgeClosed);
    }

    // ------------------------------------

    public static class Immutable<S extends IScope, L extends ILabel, O extends IOccurrence, V>
            extends EsopScopeGraph<S, L, O, V> implements IEsopScopeGraph.Immutable<S, L, O, V>, Serializable {
        private static final long serialVersionUID = 42L;

        private final Set.Immutable<S> allScopes;
        private final Set.Immutable<O> allDecls;
        private final Set.Immutable<O> allRefs;

        private final IFunction.Immutable<O, S> decls;
        private final IFunction.Immutable<O, S> refs;
        private final IRelation3.Immutable<S, L, S> directEdges;
        private final IRelation3.Immutable<O, L, S> assocEdges;
        private final IRelation3.Immutable<S, L, O> importEdges;

        private final IRelation3.Immutable<S, L, V> incompleteDirectEdges;
        private final IRelation3.Immutable<S, L, V> incompleteImportEdges;

        Immutable(Set.Immutable<S> allScopes, Set.Immutable<O> allDecls, Set.Immutable<O> allRefs,
                IFunction.Immutable<O, S> decls, IFunction.Immutable<O, S> refs,
                IRelation3.Immutable<S, L, S> directEdges, IRelation3.Immutable<O, L, S> assocEdges,
                IRelation3.Immutable<S, L, O> importEdges, IRelation3.Immutable<S, L, V> incompleteDirectEdges,
                IRelation3.Immutable<S, L, V> incompleteImportEdges) {
            super(allScopes, allDecls, allRefs, decls, refs, directEdges, assocEdges, importEdges,
                    incompleteDirectEdges, incompleteImportEdges);
            this.allScopes = allScopes;
            this.allDecls = allDecls;
            this.allRefs = allRefs;
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
            return allScopes;
        }

        @Override public Set.Immutable<O> getAllDecls() {
            return allDecls;
        }

        @Override public Set.Immutable<O> getAllRefs() {
            return allRefs;
        }

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
            return new EsopScopeGraph.Transient<>(allScopes.asTransient(), allDecls.asTransient(),
                    allRefs.asTransient(), decls.melt(), refs.melt(), directEdges.melt(), assocEdges.melt(),
                    importEdges.melt(), incompleteDirectEdges.melt(), incompleteImportEdges.melt());
        }

        public static <S extends IScope, L extends ILabel, O extends IOccurrence, V>
                EsopScopeGraph.Immutable<S, L, O, V> of() {
            return new EsopScopeGraph.Immutable<>(Set.Immutable.of(), Set.Immutable.of(), Set.Immutable.of(),
                    HashTrieFunction.Immutable.of(), HashTrieFunction.Immutable.of(), HashTrieRelation3.Immutable.of(),
                    HashTrieRelation3.Immutable.of(), HashTrieRelation3.Immutable.of(),
                    HashTrieRelation3.Immutable.of(), HashTrieRelation3.Immutable.of());
        }

    }

    public static class Transient<S extends IScope, L extends ILabel, O extends IOccurrence, V>
            extends EsopScopeGraph<S, L, O, V> implements IEsopScopeGraph.Transient<S, L, O, V> {

        private final Set.Transient<S> allScopes;
        private final Set.Transient<O> allDecls;
        private final Set.Transient<O> allRefs;

        private final IFunction.Transient<O, S> decls;
        private final IFunction.Transient<O, S> refs;
        private final IRelation3.Transient<S, L, S> directEdges;
        private final IRelation3.Transient<O, L, S> assocEdges;
        private final IRelation3.Transient<S, L, O> importEdges;

        private final IRelation3.Transient<S, L, V> incompleteDirectEdges;
        private final IRelation3.Transient<S, L, V> incompleteImportEdges;

        Transient(Set.Transient<S> allScopes, Set.Transient<O> allDecls, Set.Transient<O> allRefs,
                IFunction.Transient<O, S> decls, IFunction.Transient<O, S> refs,
                IRelation3.Transient<S, L, S> directEdges, IRelation3.Transient<O, L, S> assocEdges,
                IRelation3.Transient<S, L, O> importEdges, IRelation3.Transient<S, L, V> incompleteDirectEdges,
                IRelation3.Transient<S, L, V> incompleteImportEdges) {
            super(allScopes, allDecls, allRefs, decls, refs, directEdges, assocEdges, importEdges,
                    incompleteDirectEdges, incompleteImportEdges);
            this.allScopes = allScopes;
            this.allDecls = allDecls;
            this.allRefs = allRefs;
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
            // FIXME: check scope/D is not closed
            if(decls.put(decl, scope)) {
                allScopes.__insert(scope);
                allDecls.__insert(decl);
                return true;
            }
            return false;
        }

        @Override public boolean addRef(O ref, S scope) {
            // FIXME: check scope/R is not closed
            if(refs.put(ref, scope)) {
                allScopes.__insert(scope);
                allRefs.__insert(ref);
                return true;
            }
            return false;
        }

        @Override public boolean addDirectEdge(S sourceScope, L label, S targetScope) {
            // FIXME: check scope/l is not closed
            if(directEdges.put(sourceScope, label, targetScope)) {
                allScopes.__insert(sourceScope);
                allScopes.__insert(targetScope);
                return true;
            }
            return false;
        }

        @Override public boolean addIncompleteDirectEdge(S scope, L label, V var) {
            return incompleteDirectEdges.put(scope, label, var);
        }

        @Override public boolean addAssoc(O decl, L label, S scope) {
            // FIXME: check decl/l is not closed
            if(assocEdges.put(decl, label, scope)) {
                allScopes.__insert(scope);
                allDecls.__insert(decl);
                return true;
            }
            return false;
        }

        @Override public boolean addIncompleteImportEdge(S scope, L label, V var) {
            return incompleteImportEdges.put(scope, label, var);
        }

        @Override public boolean addImport(S scope, L label, O ref) {
            // FIXME: check scope/l is not closed
            if(importEdges.put(scope, label, ref)) {
                allScopes.__insert(scope);
                allRefs.__insert(ref);
                return true;
            }
            return false;
        }

        @Override public boolean addAll(IEsopScopeGraph<S, L, O, V> other) {
            boolean change = false;
            change |= allScopes.__insertAll(other.getAllScopes());
            change |= allDecls.__insertAll(other.getAllDecls());
            change |= allRefs.__insertAll(other.getAllRefs());
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
            return new EsopScopeGraph.Immutable<>(allScopes.freeze(), allDecls.freeze(), allRefs.freeze(),
                    decls.freeze(), refs.freeze(), directEdges.freeze(), assocEdges.freeze(), importEdges.freeze(),
                    incompleteDirectEdges.freeze(), incompleteImportEdges.freeze());
        }

        public static <S extends IScope, L extends ILabel, O extends IOccurrence, V>
                EsopScopeGraph.Transient<S, L, O, V> of() {
            return new EsopScopeGraph.Transient<>(Set.Transient.of(), Set.Transient.of(), Set.Transient.of(),
                    HashTrieFunction.Transient.of(), HashTrieFunction.Transient.of(), HashTrieRelation3.Transient.of(),
                    HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                    HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of());
        }

    }

}