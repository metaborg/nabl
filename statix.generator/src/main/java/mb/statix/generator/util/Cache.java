package mb.statix.generator.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * LRU cache with maximum size
 * Replaces a Cache from guava which was explicitly threadsafe, don't think that was relevant?
 * @param <K>
 * @param <V>
 */
public class Cache<K, V> extends LinkedHashMap<K, V> {
    public final int maximumSize;

    public Cache(int maximumSize) {
        super(16, 0.75f, true);
        if(maximumSize <= 0) {
            throw new IllegalArgumentException("Cache maximumSize should be larger than zero.");
        }
        this.maximumSize = maximumSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
       return size() > maximumSize;
    }
}
