package mb.scopegraph.pepm16.esop15.reference;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.HashTrieFunction;
import org.metaborg.util.collection.HashTrieRelation3;
import org.metaborg.util.collection.IFunction;
import org.metaborg.util.collection.IRelation3;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate3;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.tuple.Tuple2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.IndexedBagMultimap;
import mb.nabl2.util.collections.IndexedBagMultimap.RemovalPolicy;
import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.esop15.CriticalEdge;
import mb.scopegraph.pepm16.esop15.IEsopScopeGraph;

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
        incompleteDirectEdges().forEach(e -> allScopes.__insert(e.getKey()._1()));
        incompleteImportEdges().forEach(e -> allScopes.__insert(e.getKey()._1()));
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

    // ------------------------------------

    public static class Immutable<S extends IScope, L extends ILabel, O extends IOccurrence, V>
            extends EsopScopeGraph<S, L, O, V> implements IEsopScopeGraph.Immutable<S, L, O, V>, Serializable {
        private static final long serialVersionUID = 42L;

        private final IFunction.Immutable<O, S> decls;
        private final IFunction.Immutable<O, S> refs;
        private final IRelation3.Immutable<S, L, S> directEdges;
        private final IRelation3.Immutable<O, L, S> assocEdges;
        private final IRelation3.Immutable<S, L, O> importEdges;

        private final Map.Immutable<Tuple2<S, L>, V> incompleteDirectEdges;
        private final Map.Immutable<Tuple2<S, L>, V> incompleteImportEdges;

        Immutable(IFunction.Immutable<O, S> decls, IFunction.Immutable<O, S> refs,
                IRelation3.Immutable<S, L, S> directEdges, IRelation3.Immutable<O, L, S> assocEdges,
                IRelation3.Immutable<S, L, O> importEdges, Map.Immutable<Tuple2<S, L>, V> incompleteDirectEdges,
                Map.Immutable<Tuple2<S, L>, V> incompleteImportEdges) {
            this.decls = decls;
            this.refs = refs;
            this.directEdges = directEdges;
            this.assocEdges = assocEdges;
            this.importEdges = importEdges;
            this.incompleteDirectEdges = incompleteDirectEdges;
            this.incompleteImportEdges = incompleteImportEdges;
        }

        @Override public boolean isOpen(S scope, L label) {
            final Tuple2<S, L> key = Tuple2.of(scope, label);
            return incompleteDirectEdges.containsKey(key) || incompleteImportEdges.containsKey(key);
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

        @Override public Collection<? extends Entry<Tuple2<S, L>, V>> incompleteDirectEdges() {
            return incompleteDirectEdges.entrySet();
        }

        @Override public IRelation3.Immutable<O, L, S> getExportEdges() {
            return assocEdges;
        }

        @Override public IRelation3.Immutable<S, L, O> getImportEdges() {
            return importEdges;
        }

        @Override public Collection<? extends Entry<Tuple2<S, L>, V>> incompleteImportEdges() {
            return incompleteImportEdges.entrySet();
        }

        // ------------------------------------------------------------

        @Override public EsopScopeGraph.Transient<S, L, O, V> melt() {
            return new EsopScopeGraph.Transient<>(decls.melt(), refs.melt(), directEdges.melt(), assocEdges.melt(),
                    importEdges.melt(), incompleteDirectEdges.asTransient(), incompleteImportEdges.asTransient());
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
                    HashTrieRelation3.Immutable.of(), Map.Immutable.of(), Map.Immutable.of());
        }

    }

    public static class Transient<S extends IScope, L extends ILabel, O extends IOccurrence, V>
            extends EsopScopeGraph<S, L, O, V> implements IEsopScopeGraph.Transient<S, L, O, V> {

        private final IFunction.Transient<O, S> decls;
        private final IFunction.Transient<O, S> refs;
        private final IRelation3.Transient<S, L, S> directEdges;
        private final IRelation3.Transient<O, L, S> assocEdges;
        private final IRelation3.Transient<S, L, O> importEdges;

        private final IndexedBagMultimap<Tuple2<S, L>, V, V> incompleteDirectEdges;
        private final IndexedBagMultimap<Tuple2<S, L>, V, V> incompleteImportEdges;

        Transient(IFunction.Transient<O, S> decls, IFunction.Transient<O, S> refs,
                IRelation3.Transient<S, L, S> directEdges, IRelation3.Transient<O, L, S> assocEdges,
                IRelation3.Transient<S, L, O> importEdges, Map.Transient<Tuple2<S, L>, V> incompleteDirectEdges,
                Map.Transient<Tuple2<S, L>, V> incompleteImportEdges) {
            this.decls = decls;
            this.refs = refs;
            this.directEdges = directEdges;
            this.assocEdges = assocEdges;
            this.importEdges = importEdges;
            this.incompleteDirectEdges = new IndexedBagMultimap<>(RemovalPolicy.ALL);
            incompleteDirectEdges.entrySet().forEach(e -> {
                this.incompleteDirectEdges.put(Tuple2.of(e.getKey()._1(), e.getKey()._2()), e.getValue(),
                        Iterables2.singleton(e.getValue()));
            });
            this.incompleteImportEdges = new IndexedBagMultimap<>(RemovalPolicy.ALL);
            incompleteImportEdges.entrySet().forEach(e -> {
                this.incompleteImportEdges.put(Tuple2.of(e.getKey()._1(), e.getKey()._2()), e.getValue(),
                        Iterables2.singleton(e.getValue()));
            });
        }

        @Override public boolean isOpen(S scope, L label) {
            final Tuple2<S, L> key = Tuple2.of(scope, label);
            return incompleteDirectEdges.containsKey(key) || incompleteImportEdges.containsKey(key);
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

        @Override public Collection<? extends Map.Entry<Tuple2<S, L>, V>> incompleteDirectEdges() {
            return incompleteDirectEdges.entries();
        }

        @Override public IRelation3<O, L, S> getExportEdges() {
            return assocEdges;
        }

        @Override public IRelation3<S, L, O> getImportEdges() {
            return importEdges;
        }

        @Override public Collection<? extends Map.Entry<Tuple2<S, L>, V>> incompleteImportEdges() {
            return incompleteImportEdges.entries();
        }

        @Override public Iterable<V> incompleteVars() {
            return Iterables.concat(incompleteDirectEdges.indices(), incompleteImportEdges.indices());
        }

        // ------------------------------------------------------------

        @Override public boolean addDecl(S scope, O decl) {
            return decls.put(decl, scope) == null;
        }

        @Override public boolean addRef(O ref, S scope) {
            return refs.put(ref, scope) == null;
        }

        @Override public boolean addDirectEdge(S sourceScope, L label, S targetScope) {
            return directEdges.put(sourceScope, label, targetScope);
        }

        @Override public boolean addIncompleteDirectEdge(S scope, L label, V var,
                Function1<V, ? extends Set.Immutable<? extends V>> norm) {
            incompleteDirectEdges.put(Tuple2.of(scope, label), var, norm.apply(var));
            return true;
        }

        @Override public boolean addExportEdge(O decl, L label, S scope) {
            return assocEdges.put(decl, label, scope);
        }

        @Override public boolean addIncompleteImportEdge(S scope, L label, V var,
                Function1<V, ? extends Set.Immutable<? extends V>> norm) {
            incompleteImportEdges.put(Tuple2.of(scope, label), var, norm.apply(var));
            return true;
        }

        @Override public boolean addImportEdge(S scope, L label, O ref) {
            return importEdges.put(scope, label, ref);
        }

        @Override public boolean addAll(IEsopScopeGraph<S, L, O, V> other,
                Function1<V, ? extends Set.Immutable<? extends V>> norm) {
            boolean change = false;

            change |= other.getDecls().stream().filter(e -> addDecl(e._2(), e._1())).count() > 0;
            change |= other.getRefs().stream().filter(e -> addRef(e._1(), e._2())).count() > 0;

            change |= other.getDirectEdges().stream().filter(e -> addDirectEdge(e._1(), e._2(), e._3())).count() > 0;
            change |= other.getExportEdges().stream().filter(e -> addExportEdge(e._1(), e._2(), e._3())).count() > 0;
            change |= other.getImportEdges().stream().filter(e -> addImportEdge(e._1(), e._2(), e._3())).count() > 0;

            change |= other.incompleteDirectEdges().stream()
                    .filter(e -> addIncompleteDirectEdge(e.getKey()._1(), e.getKey()._2(), e.getValue(), norm))
                    .count() > 0;
            change |= other.incompleteImportEdges().stream()
                    .filter(e -> addIncompleteImportEdge(e.getKey()._1(), e.getKey()._2(), e.getValue(), norm))
                    .count() > 0;

            return change;
        }

        // -------------------------

        @Override public List<CriticalEdge> reduceAll(Function1<V, ? extends Set.Immutable<? extends V>> norm,
                Function1<V, S> fs, Function1<V, O> fo) {
            return reduce(Lists.newArrayList(incompleteVars()), norm, fs, fo);
        }

        @Override public List<CriticalEdge> reduce(Iterable<? extends V> vs,
                Function1<V, ? extends Set.Immutable<? extends V>> norm, Function1<V, S> fs, Function1<V, O> fo) {
            final ImmutableList.Builder<CriticalEdge> reduced = ImmutableList.builder();
            this.<S>reduce(incompleteDirectEdges, vs, norm, fs, this::addDirectEdge, reduced);
            this.<O>reduce(incompleteImportEdges, vs, norm, fo, this::addImportEdge, reduced);
            return reduced.build();
        }

        private <X> void reduce(IndexedBagMultimap<Tuple2<S, L>, V, V> index, Iterable<? extends V> vs,
                Function1<V, ? extends Set.Immutable<? extends V>> norm, Function1<V, X> f, Predicate3<S, L, X> add,
                ImmutableList.Builder<CriticalEdge> reduced) {
            for(V v : vs) {
                for(Map.Entry<Tuple2<S, L>, V> slv : index.reindex(v, norm)) {
                    X x = f.apply(slv.getValue());
                    S s = slv.getKey()._1();
                    L l = slv.getKey()._2();
                    add.test(s, l, x);
                    reduced.add(CriticalEdge.of(s, l));
                }
            }
        }

        // ------------------------------------------------------------

        @Override public EsopScopeGraph.Immutable<S, L, O, V> freeze() {
            return new EsopScopeGraph.Immutable<>(decls.freeze(), refs.freeze(), directEdges.freeze(),
                    assocEdges.freeze(), importEdges.freeze(), CapsuleUtil.toMap(incompleteDirectEdges.entries()),
                    CapsuleUtil.toMap(incompleteImportEdges.entries()));
        }

        public static <S extends IScope, L extends ILabel, O extends IOccurrence, V>
                EsopScopeGraph.Transient<S, L, O, V> of() {
            return new EsopScopeGraph.Transient<>(HashTrieFunction.Transient.of(), HashTrieFunction.Transient.of(),
                    HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of(),
                    HashTrieRelation3.Transient.of(), Map.Transient.of(), Map.Transient.of());
        }

    }

}
