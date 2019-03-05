package mb.nabl2.util.collections;

import java.util.Map;

public interface IInverseFunction<K, V> {

    boolean containsKey(K key);

    boolean containsEntry(K key, V value);

    boolean containsValue(V value);

    java.util.Set<K> keySet();

    java.util.Set<Map.Entry<K, V>> entrySet();

    java.util.Set<V> valueSet();

    java.util.Set<V> get(K key);

    IFunction<V, K> inverse();

    interface Immutable<K, V> extends IInverseFunction<K, V> {

        @Override
        IFunction.Immutable<V, K> inverse();

        Transient<K, V> melt();

    }

    interface Transient<K, V> extends IInverseFunction<K, V> {

        boolean put(K key, V value);

        boolean remove(K key, V value);

        @Override
        IFunction.Transient<V, K> inverse();

        Immutable<K, V> freeze();

    }

}