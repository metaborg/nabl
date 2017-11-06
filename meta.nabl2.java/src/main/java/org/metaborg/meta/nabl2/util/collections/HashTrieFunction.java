package org.metaborg.meta.nabl2.util.collections;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.SetMultimap;

public abstract class HashTrieFunction<K, V> implements IFunction<K, V> {

    protected HashTrieFunction() {
    }

    protected abstract Map<K, V> fwd();

    protected abstract SetMultimap<V, K> bwd();

    @Override public boolean containsKey(K key) {
        return fwd().containsKey(key);
    }

    @Override public boolean containsValue(V value) {
        return bwd().containsKey(value);
    }

    @Override public boolean containsEntry(K key, V value) {
        return bwd().containsEntry(value, key);
    }

    @Override public java.util.Set<K> keySet() {
        return fwd().keySet();
    }

    @Override public Set<Map.Entry<K, V>> entrySet() {
        return fwd().entrySet();
    }

    @Override public java.util.Set<V> valueSet() {
        return bwd().keySet();
    }

    @Override public Optional<V> get(K key) {
        return Optional.ofNullable(fwd().get(key));
    }

    @Override public String toString() {
        return fwd().toString();
    }


    public static class Immutable<K, V> extends HashTrieFunction<K, V>
            implements IFunction.Immutable<K, V>, Serializable {
        private static final long serialVersionUID = 42L;

        private final Map.Immutable<K, V> fwd;
        private final SetMultimap.Immutable<V, K> bwd;

        Immutable(Map.Immutable<K, V> fwd, SetMultimap.Immutable<V, K> bwd) {
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override protected Map<K, V> fwd() {
            return fwd;
        }

        @Override protected SetMultimap<V, K> bwd() {
            return bwd;
        }

        @Override public HashTrieInverseFunction.Immutable<V, K> inverse() {
            return new HashTrieInverseFunction.Immutable<>(bwd, fwd);
        }

        @Override public HashTrieFunction.Transient<K, V> melt() {
            return new HashTrieFunction.Transient<>(fwd.asTransient(), bwd.asTransient());
        }

        public static <K, V> HashTrieFunction.Immutable<K, V> of() {
            return new HashTrieFunction.Immutable<>(Map.Immutable.of(), SetMultimap.Immutable.of());
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + bwd.hashCode();
            result = prime * result + fwd.hashCode();
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") final HashTrieFunction.Immutable<K, V> other =
                    (HashTrieFunction.Immutable<K, V>) obj;
            if(!bwd.equals(other.bwd))
                return false;
            if(!fwd.equals(other.fwd))
                return false;
            return true;
        }

    }


    public static class Transient<K, V> extends HashTrieFunction<K, V> implements IFunction.Transient<K, V> {

        private final Map.Transient<K, V> fwd;
        private final SetMultimap.Transient<V, K> bwd;

        Transient(Map.Transient<K, V> fwd, SetMultimap.Transient<V, K> bwd) {
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override protected Map<K, V> fwd() {
            return fwd;
        }

        @Override protected SetMultimap<V, K> bwd() {
            return bwd;
        }

        @Override public boolean put(K key, V value) {
            if(fwd.containsKey(key)) {
                if(value.equals(fwd.get(key))) {
                    return false;
                } else {
                    throw new IllegalArgumentException("Already in domain.");
                }
            }
            fwd.__put(key, value);
            bwd.__insert(value, key);
            return true;
        }

        @Override public boolean putAll(IFunction<K, V> other) {
            return other.stream().reduce(false, (change, kv) -> Boolean.logicalOr(change, put(kv._1(), kv._2())),
                    Boolean::logicalOr);
        }

        @Override public boolean remove(K key) {
            V value = fwd.__remove(key);
            if(value != null) {
                bwd.__remove(value, key);
                return true;
            }
            return false;
        }

        @Override public HashTrieInverseFunction.Transient<V, K> inverse() {
            return new HashTrieInverseFunction.Transient<>(bwd, fwd);
        }

        @Override public HashTrieFunction.Immutable<K, V> freeze() {
            return new HashTrieFunction.Immutable<>(fwd.freeze(), bwd.freeze());
        }

        public static <K, V> HashTrieFunction.Transient<K, V> of() {
            return new HashTrieFunction.Transient<>(Map.Transient.of(), SetMultimap.Transient.of());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((bwd == null) ? 0 : bwd.hashCode());
            result = prime * result + ((fwd == null) ? 0 : fwd.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            HashTrieFunction.Transient other = (HashTrieFunction.Transient) obj;
            if (bwd == null) {
                if (other.bwd != null)
                    return false;
            } else if (!bwd.equals(other.bwd))
                return false;
            if (fwd == null) {
                if (other.fwd != null)
                    return false;
            } else if (!fwd.equals(other.fwd))
                return false;
            return true;
        }

    }


    public static <K, V> IFunction<K, V> union(IFunction<K, V> fun1, IFunction<K, V> fun2) {
        return new Union<>(fun1, fun2);
    }

    private static class Union<K, V> implements IFunction<K, V> {

        private final IFunction<K, V> fun1;
        private final IFunction<K, V> fun2;

        private Union(IFunction<K, V> fun1, IFunction<K, V> fun2) {
            this.fun1 = fun1;
            this.fun2 = fun2;
        }

        @Override public IInverseFunction<V, K> inverse() {
            return HashTrieInverseFunction.union(fun1.inverse(), fun2.inverse());
        }

        @Override public boolean containsKey(K key) {
            return fun1.containsKey(key) || fun2.containsKey(key);
        }

        @Override public boolean containsEntry(K key, V value) {
            return fun1.containsEntry(key, value) || fun2.containsEntry(key, value);
        }

        @Override public boolean containsValue(V value) {
            return fun1.containsValue(value) || fun2.containsValue(value);
        }

        @Override public java.util.Set<K> keySet() {
            return Sets.union(fun1.keySet(), fun2.keySet());
        }

        @Override public java.util.Set<Map.Entry<K, V>> entrySet() {
            return Sets.union(fun1.entrySet(), fun2.entrySet());
        }

        @Override public java.util.Set<V> valueSet() {
            return Sets.union(fun1.valueSet(), fun2.valueSet());
        }

        @Override public Optional<V> get(K key) {
            return fun1.get(key).map(Optional::of).orElseGet(() -> fun2.get(key));
        }

    }

}
