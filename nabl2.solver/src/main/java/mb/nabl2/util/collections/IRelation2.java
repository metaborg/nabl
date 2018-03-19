package mb.nabl2.util.collections;

import java.util.Map;

import com.google.common.annotations.Beta;

import mb.nabl2.util.Tuple2;

public interface IRelation2<K, V> {

    IRelation2<V, K> inverse();

    boolean containsKey(K key);

    boolean containsEntry(K key, V value);

    boolean containsValue(V value);

    int size();

    boolean isEmpty();

    java.util.Set<V> get(K key);

    java.util.Set<K> keySet();

    java.util.Set<V> valueSet();

    java.util.Set<Map.Entry<K, V>> entrySet();

    @Beta default java.util.stream.Stream<Tuple2<K, V>> stream() {
        return this.entrySet().stream().map(Tuple2::of);
    }

    interface Immutable<K, V> extends IRelation2<K, V> {

        IRelation2.Immutable<V, K> inverse();

        IRelation2.Transient<K, V> melt();

    }

    interface Transient<K, V> extends IRelation2<K, V> {

        IRelation2.Transient<V, K> inverse();

        boolean put(K key, V value);

        boolean putAll(K key, Iterable<? extends V> values);

        boolean putAll(IRelation2<K, V> other);

        java.util.Set<V> removeKey(K key);

        java.util.Set<K> removeValue(V value);

        boolean removeEntry(K key, V value);

        IRelation2.Immutable<K, V> freeze();

    }

}