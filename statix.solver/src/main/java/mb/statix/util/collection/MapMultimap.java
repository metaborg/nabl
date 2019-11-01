package mb.statix.util.collection;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import mb.nabl2.util.ImmutableTuple3;
import mb.nabl2.util.Tuple3;
import mb.statix.modular.util.TPrettyPrinter;
import mb.statix.util.function.SerializableFunction;
import mb.statix.util.function.SerializableSupplier;
import mb.statix.util.function.TriConsumer;

/**
 * Note that MapMultimap is best suited when there are many {@code K2} entries per {@code K1} key.
 * 
 * @param <K1> the type of the first key
 * @param <K2> the type of the second key
 * @param <V> the type of values
 */
public class MapMultimap<K1, K2, V> implements Iterable<Tuple3<K1, K2, V>>, Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Map<K1, Multimap<K2, V>> map;
    private final SerializableFunction<K1, Multimap<K2, V>> function;
    
    public MapMultimap() {
        this.map = new HashMap<>();
        this.function = x -> MultimapBuilder.hashKeys().hashSetValues().build();
    }
    
    public MapMultimap(Map<K1, Multimap<K2, V>> baseMap) {
        this.map = baseMap;
        this.function = x -> MultimapBuilder.hashKeys().hashSetValues().build();
    }
    
    public MapMultimap(Map<K1, Multimap<K2, V>> baseMap, SerializableSupplier<Multimap<K2, V>> supplier) {
        this.map = baseMap;
        this.function = x -> supplier.get();
    }
    
    public MapMultimap(Map<K1, Multimap<K2, V>> baseMap, SerializableFunction<K1, Multimap<K2, V>> function) {
        this.map = baseMap;
        this.function = function;
    }
    
    public boolean isEmpty() {
        return map.isEmpty();
    }
    
    public Collection<? extends Entry<K2, V>> get(K1 k1) {
        Multimap<K2, V> nmap = map.get(k1);
        if (nmap == null) return Collections.emptySet();
        return nmap.entries();
    }
    
    public Multimap<K2, V> get2(K1 k1) {
        Multimap<K2, V> nmap = map.get(k1);
        if (nmap == null) return ImmutableMultimap.of();
        return nmap;
    }
    
    public Collection<V> get(K1 k1, K2 k2) {
        Multimap<K2, V> nmap = map.get(k1);
        if (nmap == null) return Collections.emptySet();
        return nmap.get(k2);
    }
    
    public boolean contains(K1 k1) {
        return map.containsKey(k1);
    }
    
    public boolean contains(K1 k1, K2 k2) {
        Multimap<K2, V> nmap = map.get(k1);
        return nmap == null ? false : nmap.containsKey(k2);
    }
    
    public boolean contains(K1 k1, K2 k2, V v) {
        Multimap<K2, V> nmap = map.get(k1);
        return nmap == null ? false : nmap.containsEntry(k2, v);
    }
    
    public boolean put(K1 k1, K2 k2, V v) {
        Multimap<K2, V> nmap = map.computeIfAbsent(k1, function);
        return nmap.put(k2, v);
    }
    
    public Multimap<K2, V> removeAll(K1 k1) {
        Multimap<K2, V> nmap = map.remove(k1);
        if (nmap == null) return ImmutableMultimap.of();
        return nmap;
    }
    
    public Collection<V> removeAll(K1 k1, K2 k2) {
        Multimap<K2, V> nmap = map.get(k1);
        if (nmap == null) return Collections.emptySet();
        return nmap.removeAll(k2);
    }
    
    public boolean remove(K1 k1, K2 k2, V v) {
        Multimap<K2, V> nmap = map.get(k1);
        if (nmap == null) return false;
        return nmap.remove(k2, v);
    }
    
    public void clear() {
        map.clear();
    }
    
    public Set<K1> keySet() {
        return map.keySet();
    }
    
    public Set<Entry<K1, Multimap<K2, V>>> entrySet() {
        return map.entrySet();
    }
    
    public Set<Tuple3<K1, K2, V>> entries() {
        return stream().collect(Collectors.toSet());
    }
    
    @Override
    public Iterator<Tuple3<K1, K2, V>> iterator() {
        return stream().iterator();
    }
    
    public Stream<Tuple3<K1, K2, V>> stream() {
        return map.entrySet().stream()
                .flatMap(e1 -> e1.getValue().entries().stream()
                        .map(e2 -> ImmutableTuple3.of(e1.getKey(), e2.getKey(), e2.getValue())));
    }
    
    /**
     * @deprecated Use {@link #forEach(TriConsumer)} instead.
     */
    @Override @Deprecated
    public void forEach(Consumer<? super Tuple3<K1, K2, V>> action) {
        for (K1 k1 : map.keySet()) {
            Multimap<K2, V> nmap = map.get(k1);
            for (Entry<K2, V> entry : nmap.entries()) {
                action.accept(ImmutableTuple3.of(k1, entry.getKey(), entry.getValue()));
            }
        }
    }
    
    public void forEach(TriConsumer<K1, K2, V> action) {
        for (K1 k1 : map.keySet()) {
            Multimap<K2, V> nmap = map.get(k1);
            for (Entry<K2, V> entry : nmap.entries()) {
                action.accept(k1, entry.getKey(), entry.getValue());
            }
        }
    }
    
    @Override
    public String toString() {
        return TPrettyPrinter.prettyPrint(map);
    }
    
    /**
     * Creates a new concurrent {@link MapMultimap}, using a {@link ConcurrentHashMap} as a backing
     * map and synchronized multimaps from the given builder as backing multimaps.
     * 
     * @param function
     *      the function to create (concurrent!) multimaps with
     * 
     * @return
     *      a concurrent {@link MapMultimap}
     */
    public static <K1, K2, V> MapMultimap<K1, K2, V> concurrent(SerializableFunction<K1, Multimap<K2, V>> function) {
        return new MapMultimap<>(new ConcurrentHashMap<>(), function);
    }
}
