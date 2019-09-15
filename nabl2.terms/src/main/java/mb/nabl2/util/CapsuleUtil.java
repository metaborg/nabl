package mb.nabl2.util;

import java.util.Map.Entry;

import org.metaborg.util.functions.Function2;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;

public final class CapsuleUtil {

    private CapsuleUtil() {
    }

    public static <K, V> void replace(Map.Transient<K, V> map, Function2<K, V, V> mapper) {
        for(Entry<K, V> entry : map.entrySet()) {
            final K key = entry.getKey();
            final V val = mapper.apply(key, entry.getValue());
            if(val != null) {
                map.__put(key, val);
            } else {
                map.__remove(key);
            }
        }
    }

    public static <K, V> Map.Immutable<K, V> replace(Map.Immutable<K, V> map, Function2<K, V, V> mapper) {
        final Map.Transient<K, V> newMap = Map.Transient.of();
        for(Entry<K, V> entry : map.entrySet()) {
            final K key = entry.getKey();
            final V val = mapper.apply(key, entry.getValue());
            if(val != null) {
                newMap.__put(key, val);
            }
        }
        return newMap.freeze();
    }

    @SuppressWarnings("unchecked") public static <V> Set.Immutable<V> toSet(Iterable<? extends V> values) {
        if(values instanceof Set.Immutable) {
            return (Set.Immutable<V>) values;
        }
        Set.Transient<V> set = Set.Transient.of();
        for(V value : values) {
            set.__insert(value);
        }
        return set.freeze();
    }

    @SuppressWarnings("unchecked") public static <K, V> Map.Immutable<K, V> toMap(java.util.Map<K, V> map) {
        return (Map.Immutable<K, V>) Map.Immutable.of().__putAll(map);
    }

}