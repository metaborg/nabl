package org.metaborg.meta.nabl2.util.collections;

public interface IInverseFunction<K, V> {

    boolean containsKey(K key);

    boolean containsEntry(K key, V value);

    boolean containsValue(V value);

    java.util.Set<K> keySet();

    java.util.Set<V> valueSet();

    java.util.Set<V> get(K key);

    IFunction<V, K> inverse();

    interface Immutable<K, V> extends IInverseFunction<K, V> {

        IFunction.Immutable<V, K> inverse();

        Transient<K, V> melt();

    }

    interface Transient<K, V> extends IInverseFunction<K, V> {

        boolean put(K key, V value);

        boolean remove(K key, V value);

        IFunction.Transient<V, K> inverse();

        Immutable<K, V> freeze();

    }

}