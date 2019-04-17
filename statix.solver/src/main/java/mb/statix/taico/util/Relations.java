package mb.statix.taico.util;

import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Sets;

import mb.nabl2.util.collections.IRelation3;

public class Relations {
    /**
     * Creates a view of the given relation3s. The view will always reflect the latest state of
     * both relation3s.
     * 
     * @param a
     *      the first relation3
     * @param b
     *      the second relation3
     * 
     * @return
     *      a view on the given relations
     */
    public static <K, L, V> IRelation3View<K, L, V> union(IRelation3<K, L, V> a, IRelation3<K, L, V> b) {
        return new IRelation3View<>(a, b);
    }
    
    private static class IRelation3View<K, L, V> implements IRelation3<K, L, V> {
        private final IRelation3<K, L, V> a, b;
        
        public IRelation3View(IRelation3<K, L, V> a, IRelation3<K, L, V> b) {
            this.a = a;
            this.b = b;
        }
        
        @Override
        public IRelation3<V, L, K> inverse() {
            return new IRelation3View<V, L, K>(a.inverse(), b.inverse());
        }

        @Override
        public Set<K> keySet() {
            return Sets.union(a.keySet(), b.keySet());
        }

        @Override
        public Set<V> valueSet() {
            return Sets.union(a.valueSet(), b.valueSet());
        }

        @Override
        public boolean contains(K key) {
            return a.contains(key) || b.contains(key);
        }

        @Override
        public boolean contains(K key, L label) {
            return a.contains(key, label) || b.contains(key, label);
        }

        @Override
        public boolean contains(K key, L label, V value) {
            return a.contains(key, label, value) || b.contains(key, label, value);
        }

        @Override
        public boolean isEmpty() {
            return a.isEmpty() && b.isEmpty();
        }

        @Override
        public Set<? extends Entry<L, V>> get(K key) {
            return Sets.union(a.get(key), b.get(key));
        }

        @Override
        public Set<V> get(K key, L label) {
            return Sets.union(a.get(key, label), b.get(key, label));
        }
    }
}
