package mb.nabl2.scopegraph.esop.reference;

import java.io.Serializable;
import java.util.stream.Stream;

import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.functions.Predicate3;

import io.usethesource.capsule.Set;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph;
import mb.nabl2.util.collections.HashTrieFunction;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IFunction;
import mb.nabl2.util.collections.IRelation3;

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

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + decls.hashCode();
            result = prime * result + refs.hashCode();
            result = prime * result + directEdges.hashCode();
            result = prime * result + assocEdges.hashCode();
            result = prime * result + importEdges.hashCode();
            result = prime * result + incompleteDirectEdges.hashCode();
            result = prime * result + incompleteImportEdges.hashCode();
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") EsopScopeGraph.Immutable<S, L, O, V> other =
                    (EsopScopeGraph.Immutable<S, L, O, V>) obj;
            if(!decls.equals(other.decls))
                return false;
            if(!refs.equals(other.refs))
                return false;
            if(!directEdges.equals(other.directEdges))
                return false;
            if(!assocEdges.equals(other.assocEdges))
                return false;
            if(!importEdges.equals(other.importEdges))
                return false;
            if(!incompleteDirectEdges.equals(other.incompleteDirectEdges))
                return false;
            if(!incompleteImportEdges.equals(other.incompleteImportEdges))
                return false;
            return true;
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
            }).count() != 0; // Do not use findAny().isPresent(), which short-cuts the side-effecty flatMap+map
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
