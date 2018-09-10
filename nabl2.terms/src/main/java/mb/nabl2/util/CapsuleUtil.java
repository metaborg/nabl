package mb.nabl2.util;

import java.util.Map.Entry;

import org.metaborg.util.functions.Function2;

import io.usethesource.capsule.Map;

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

}