package mb.nabl2.util;

import java.util.Map.Entry;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Function2;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;

public final class CapsuleUtil {

    private CapsuleUtil() {
    }

    /**
     * Replace the entry's value, or keep original value if the function returns `null`.
     */
    public static <K, V> void updateValues(Map.Transient<K, V> map, Function2<K, V, V> mapper) {
        for(Entry<K, V> entry : map.entrySet()) {
            final K key = entry.getKey();
            final V val = mapper.apply(key, entry.getValue());
            if(val != null) {
                map.__put(key, val);
            }
        }
    }

    /**
     * Replace the entry's value, or remove entry if the function returns `null`.
     */
    public static <K, V> void updateValuesOrRemove(Map.Transient<K, V> map, Function2<K, V, V> mapper) {
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

    /**
     * Replace the entry's key, or keep original value if the function returns `null`.
     */
    public static <K, V> void updateKeys(Map.Transient<K, V> map, Function2<K, V, K> mapper) {
        for(Entry<K, V> entry : map.entrySet()) {
            final K key = entry.getKey();
            final K newKey = mapper.apply(key, entry.getValue());
            if(newKey != null) {
                map.__remove(key);
                map.__put(newKey, entry.getValue());
            }
        }
    }

    /**
     * Replace the entry's key, or remove entry if the function returns `null`.
     */
    public static <K, V> void updateKeysOrRemove(Map.Transient<K, V> map, Function2<K, V, K> mapper) {
        for(Entry<K, V> entry : map.entrySet()) {
            final K key = entry.getKey();
            final K newKey = mapper.apply(key, entry.getValue());
            map.__remove(key);
            if(newKey != null) {
                map.__put(newKey, entry.getValue());
            }
        }
    }

    /**
     * Replace the set values, or keep if the function returns `null`.
     */
    public static <K, V> void update(Set.Transient<V> set, Function1<V, V> mapper) {
        for(V val : set) {
            final V newVal = mapper.apply(val);
            if(newVal != null) {
                set.__remove(val);
                set.__insert(newVal);
            }
        }
    }

    /**
     * Replace the set values, or remove if the function returns `null`.
     */
    public static <K, V> void updateOrRemove(Set.Transient<V> set, Function1<V, V> mapper) {
        for(V val : set) {
            final V newVal = mapper.apply(val);
            set.__remove(val);
            if(newVal != null) {
                set.__insert(newVal);
            }
        }
    }

    @SuppressWarnings("unchecked") public static <V> Set.Immutable<V> toSet(Iterable<? extends V> values) {
        if(values instanceof Set.Immutable) {
            return (Set.Immutable<V>) values;
        }
        final Set.Transient<V> set = Set.Transient.of();
        values.forEach(set::__insert);
        return set.freeze();
    }

    @SuppressWarnings("unchecked") public static <K, V> Map.Immutable<K, V>
            toMap(java.util.Map<? extends K, ? extends V> map) {
        if(map instanceof Map.Immutable) {
            return (Map.Immutable<K, V>) map;
        }
        return (Map.Immutable<K, V>) Map.Immutable.of().__putAll(map);
    }

    public static <K, V> Map.Immutable<K, V> toMap(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
        final Map.Transient<K, V> map = Map.Transient.of();
        entries.forEach(e -> map.__put(e.getKey(), e.getValue()));
        return map.freeze();
    }

    @SuppressWarnings("unchecked") public static <K, V> SetMultimap.Immutable<K, V>
            toSetMultimap(SetMultimap<? extends K, ? extends V> map) {
        if(map instanceof SetMultimap.Immutable) {
            return (SetMultimap.Immutable<K, V>) map;
        }
        final SetMultimap.Transient<K, V> multimap = SetMultimap.Transient.of();
        map.entrySet().forEach(e -> multimap.__insert(e.getKey(), e.getValue()));
        return multimap.freeze();
    }

}