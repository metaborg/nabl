package mb.nabl2.util.collections;

import java.util.Map;
import java.util.WeakHashMap;

public class ConcurrentWeakCache<K, V> {

    private final Map<K, V> entries = new WeakHashMap<>();

    public synchronized int size() {
        return entries.size();
    }

    public synchronized boolean isEmpty() {
        return entries.isEmpty();
    }

    public synchronized V getOrPut(K key, V value) {
        V oldValue = entries.putIfAbsent(key, value);
        return oldValue != null ? oldValue : value;
    }

    public synchronized void clear() {
        entries.clear();
    }

}