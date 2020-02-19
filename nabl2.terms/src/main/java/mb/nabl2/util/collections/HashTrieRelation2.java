package mb.nabl2.util.collections;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;

public abstract class HashTrieRelation2<K, V> implements IRelation2<K, V> {

    protected HashTrieRelation2() {
    }

    protected abstract SetMultimap<K, V> fwd();

    protected abstract SetMultimap<V, K> bwd();

    @Override public boolean containsKey(K key) {
        return fwd().containsKey(key);
    }

    @Override public boolean containsEntry(K key, V value) {
        return fwd().containsEntry(key, value);
    }

    @Override public boolean containsValue(V value) {
        return bwd().containsKey(value);
    }

    @Override public java.util.Set<K> keySet() {
        return fwd().keySet();
    }

    @Override public java.util.Set<V> valueSet() {
        return bwd().keySet();
    }

    @Override public java.util.Set<Map.Entry<K, V>> entrySet() {
        return fwd().entrySet();
    }

    @Override public Set.Immutable<V> get(K key) {
        return fwd().get(key);
    }

    @Override public int size() {
        return fwd().size();
    }

    @Override public boolean isEmpty() {
        return fwd().isEmpty();
    }

    @Override public String toString() {
        return fwd().toString();
    }


    public static class Immutable<K, V> extends HashTrieRelation2<K, V>
            implements IRelation2.Immutable<K, V>, Serializable {
        private static final long serialVersionUID = 42L;

        private final SetMultimap.Immutable<K, V> fwd;
        private final SetMultimap.Immutable<V, K> bwd;

        Immutable(SetMultimap.Immutable<K, V> fwd, SetMultimap.Immutable<V, K> bwd) {
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override protected SetMultimap<K, V> fwd() {
            return fwd;
        }

        @Override protected SetMultimap<V, K> bwd() {
            return bwd;
        }

        @Override public IRelation2.Immutable<V, K> inverse() {
            return new HashTrieRelation2.Immutable<>(bwd, fwd);
        }

        @Override public HashTrieRelation2.Transient<K, V> melt() {
            return new HashTrieRelation2.Transient<>(fwd.asTransient(), bwd.asTransient());
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + fwd.hashCode();
            result = prime * result + bwd.hashCode();
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") final HashTrieRelation2.Immutable<K, V> other =
                    (HashTrieRelation2.Immutable<K, V>) obj;
            if(!fwd.equals(other.fwd))
                return false;
            if(!bwd.equals(other.bwd))
                return false;
            return true;
        }

        public static <K, V> HashTrieRelation2.Immutable<K, V> of() {
            return new HashTrieRelation2.Immutable<>(SetMultimap.Immutable.of(), SetMultimap.Immutable.of());
        }

    }


    public static class Transient<K, V> extends HashTrieRelation2<K, V> implements IRelation2.Transient<K, V> {

        private final SetMultimap.Transient<K, V> fwd;
        private final SetMultimap.Transient<V, K> bwd;

        Transient(SetMultimap.Transient<K, V> fwd, SetMultimap.Transient<V, K> bwd) {
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override protected SetMultimap<K, V> fwd() {
            return fwd;
        }

        @Override protected SetMultimap<V, K> bwd() {
            return bwd;
        }

        @Override public boolean put(K key, V value) {
            if(fwd.__insert(key, value)) {
                bwd.__insert(value, key);
                return true;

            }
            return false;
        }

        @Override public boolean putAll(K key, Iterable<? extends V> values) {
            boolean change = false;
            for(V value : values) {
                change |= put(key, value);
            }
            return change;
        }

        @Override public boolean putAll(IRelation2<K, V> other) {
            return other.stream().reduce(false, (change, klv) -> Boolean.logicalOr(change, put(klv._1(), klv._2())),
                    Boolean::logicalOr);
        }

        @Override public Set.Immutable<V> removeKey(K key) {
            final Set.Immutable<V> values = fwd.get(key);
            fwd.__remove(key);
            for(V value : values) {
                bwd.__remove(value, key);
            }
            return values;
        }

        @Override public Set.Immutable<K> removeValue(V value) {
            final Set.Immutable<K> keys = bwd.get(value);
            bwd.__remove(value);
            for(K key : keys) {
                fwd.__remove(key, value);
            }
            return keys;
        }

        @Override public boolean removeEntry(K key, V value) {
            if(fwd.__remove(key, value)) {
                bwd.__remove(value, key);
                return true;
            }
            return false;
        }

        @Override public IRelation2.Transient<V, K> inverse() {
            return new HashTrieRelation2.Transient<>(bwd, fwd);
        }

        @Override public HashTrieRelation2.Immutable<K, V> freeze() {
            return new HashTrieRelation2.Immutable<>(fwd.freeze(), bwd.freeze());
        }

        public static <K, V> HashTrieRelation2.Transient<K, V> of() {
            return new HashTrieRelation2.Transient<>(SetMultimap.Transient.of(), SetMultimap.Transient.of());
        }

    }


    public static <K, V> IRelation2<K, V> union(IRelation2<K, V> rel1, IRelation2<K, V> rel2) {
        return new Union<>(rel1, rel2);
    }

    private static class Union<K, V> implements IRelation2<K, V> {

        private final IRelation2<K, V> rel1;
        private final IRelation2<K, V> rel2;

        private Union(IRelation2<K, V> rel1, IRelation2<K, V> rel2) {
            this.rel1 = rel1;
            this.rel2 = rel2;
        }

        @Override public IRelation2<V, K> inverse() {
            return new Union<>(rel1.inverse(), rel2.inverse());
        }

        @Override public boolean containsKey(K key) {
            return rel1.containsKey(key) || rel2.containsKey(key);
        }

        @Override public boolean containsEntry(K key, V value) {
            return rel1.containsEntry(key, value) || rel2.containsEntry(key, value);
        }

        @Override public boolean containsValue(V value) {
            return rel1.containsValue(value) || rel2.containsValue(value);
        }

        @Override public int size() {
            return rel1.size() + rel2.size();
        }

        @Override public boolean isEmpty() {
            return rel1.isEmpty() && rel2.isEmpty();
        }

        @Override public java.util.Set<V> get(K key) {
            return Sets.union(rel1.get(key), rel2.get(key));
        }

        @Override public java.util.Set<K> keySet() {
            return Sets.union(rel1.keySet(), rel2.keySet());
        }

        @Override public java.util.Set<V> valueSet() {
            return Sets.union(rel1.valueSet(), rel2.valueSet());
        }

        @Override public java.util.Set<Entry<K, V>> entrySet() {
            return Sets.union(rel1.entrySet(), rel2.entrySet());
        }

    }

}
