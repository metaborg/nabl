package mb.scopegraph.patching;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Iterators;

class IdentityMappingEntrySet<E> extends AbstractSet<Map.Entry<E, E>> {

    private final Set<E> backingSet;

    IdentityMappingEntrySet(Set<E> backingSet) {
        this.backingSet = backingSet;
    }

    @Override public int size() {
        return backingSet.size();
    }

    @Override public boolean isEmpty() {
        return backingSet.isEmpty();
    }

    @Override public boolean contains(Object o) {
        if(!(o instanceof Entry<?, ?>)) {
            return false;
        }
        final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
        if(!isIdentity(entry)) {
            return false;
        }
        return backingSet.contains(entry.getKey());
    }

    @Override public Iterator<Entry<E, E>> iterator() {
        return Iterators.transform(backingSet.iterator(), elem -> new AbstractMap.SimpleImmutableEntry<E, E>(elem, elem));
    }

    @Override public boolean add(Entry<E, E> entry) {
        if(isIdentity(entry)) {
            return backingSet.add(entry.getKey());
        }
        return throwNotIdentityExpection(entry);
    }

    @Override public boolean remove(Object o) {
        if(!(o instanceof Entry<?, ?>)) {
            return false;
        }
        final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
        if(isIdentity(entry)) {
            return backingSet.remove(entry.getKey());
        }
        return throwNotIdentityExpection(entry);
    }

    @Override public void clear() {
        backingSet.clear();
    }

    private static boolean isIdentity(Entry<?, ?> entry) {
        return entry.getKey().equals(entry.getValue());
    }

    private static boolean throwNotIdentityExpection(Entry<?, ?> entry) {
        throw new UnsupportedOperationException("Expected identity mapping, but was: " + entry);
    }

}
