package mb.statix.taico.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TOverrides {
    /** Redirect STX_solve_constraint to MSTX_solve_constraint. */
    public static boolean MODULES_OVERRIDE = true;
    /** If the log level should be overridden to the value below. */
    public static boolean OVERRIDE_LOGLEVEL = true;
    /** The log level to use, has no effect if OVERRIDE_LOGLEVEL is false. */
    public static String LOGLEVEL = "none"; //"debug" "none"
    /** If concurrency should be used. Uses the number of threads below. */
    public static boolean CONCURRENT = true;
    /** The number of threads for concurrency. Has no effect if CONCURRENT is false. */
    public static int THREADS = 4;
    
    /**
     * If true, the observer mechanism is used for own critical edges.
     * Otherwise, the"just redo whenever the critical edge MIGHT have changed" variant is used. 
     */
    public static boolean USE_OBSERVER_MECHANISM_FOR_SELF = true;
    
    /** If true, will cause the statix calls to fail in order to trigger a clean run. */
    public static boolean CLEAN = false;
    
    public static String print() {
        return "Concurrent=" + (CONCURRENT ? THREADS : "false") +
                ", Loglevel=" + (OVERRIDE_LOGLEVEL ? LOGLEVEL : "not overridden") +
                ", UseObserverMechanismSelf=" + USE_OBSERVER_MECHANISM_FOR_SELF;
    }
    
    /**
     * If concurrency is enabled, this method returns a {@link ConcurrentHashMap}. Otherwise, this
     * method returns a normal {@link HashMap}.
     * 
     * @return
     *      either a concurrent or non concurrent hash map
     */
    public static <K, V> Map<K, V> hashMap() {
        return CONCURRENT ? new ConcurrentHashMap<>() : new HashMap<>();
    }
    
    /**
     * If concurrency is enabled, this method returns a {@link ConcurrentHashMap}. Otherwise, this
     * method returns a normal {@link HashMap}.
     * 
     * All the mappings in the given map are added to the returned map.
     * 
     * @param map
     *      the map
     * 
     * @return
     *      either a concurrent or non concurrent hash map
     */
    public static <K, V> Map<K, V> hashMap(Map<K, V> map) {
        return CONCURRENT ? new ConcurrentHashMap<>(map) : new HashMap<>(map);
    }
    
    /**
     * If concurrency is enabled, this method returns a concurrent hash set. Otherwise, this
     * method returns a normal {@link HashSet}.
     * 
     * @return
     *      either a concurrent or non concurrent set
     */
    public static <E> Set<E> set() {
        return CONCURRENT ? ConcurrentHashMap.newKeySet() : new HashSet<>();
    }
    
    /**
     * If concurrency is enabled, this method returns a concurrent hash set. Otherwise, this
     * method returns a normal {@link HashSet}.
     * 
     * The given elements are added to the set.
     * 
     * @param elements
     *      the elements
     * 
     * @return
     *      either a concurrent or non concurrent set
     */
    public static <E> Set<E> set(Collection<E> elements) {
        if (CONCURRENT) {
            Set<E> set = ConcurrentHashMap.newKeySet();
            set.addAll(elements);
            return set;
        }
        
        return new HashSet<>(elements);
    }
}
