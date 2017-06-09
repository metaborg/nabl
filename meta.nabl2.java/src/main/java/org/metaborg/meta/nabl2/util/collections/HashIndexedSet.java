package org.metaborg.meta.nabl2.util.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class HashIndexedSet<E, I> implements IIndexedSet.Mutable<E, I> {

    private final Function1<E, I> indexer;
    private final SetMultimap<I, E> elems;

    public HashIndexedSet(Function1<E, I> indexer) {
        this.indexer = indexer;
        this.elems = HashMultimap.create();
    }

    @Override public boolean containsElement(E elem) {
        return elems.containsValue(elem);
    }

    @Override public boolean containsIndex(I index) {
        return elems.containsKey(index);
    }

    @Override public Set<E> get(I index) {
        return Collections.unmodifiableSet(elems.get(index));
    }

    @Override public Set<I> indices() {
        return Collections.unmodifiableSet(elems.keySet());
    }

    @Override public Collection<E> values() {
        return Collections.unmodifiableCollection(elems.values());
    }

    @Override public boolean add(E elem) {
        return elems.put(indexer.apply(elem), elem);
    }

    @Override public boolean remove(E elem) {
        return elems.remove(indexer.apply(elem), elem);
    }

}
