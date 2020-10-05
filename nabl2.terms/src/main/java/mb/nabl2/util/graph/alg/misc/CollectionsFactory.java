/*******************************************************************************
 * Copyright (c) 2010-2013, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package mb.nabl2.util.graph.alg.misc;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Factory class used as an accessor to Collections implementations.
 * 
 * @author istvanrath
 */
public final class CollectionsFactory {

    /**
     * Instantiates a new empty map.
     * 
     * @since 1.7
     */
    public static <K, V> Map<K, V> createMap() {
        return Maps.newHashMap();
    }

    /**
     * Instantiates a new map with the given initial contents.
     * 
     * @since 1.7
     */
    public static <K, V> Map<K, V> createMap(Map<K, V> initial) {
        return Maps.newHashMap(initial);
    }

    /**
     * Instantiates a new empty set.
     * 
     * @since 1.7
     */
    public static <E> Set<E> createSet() {
        return Sets.newHashSet();
    }

    /**
     * Instantiates a new set with the given initial contents.
     * 
     * @since 1.7
     */
    public static <E> Set<E> createSet(Collection<E> initial) {
        return Sets.newHashSet(initial);
    }

    /**
     * Instantiates a new list that is optimized for registering observers / callbacks.
     * 
     * @since 1.7
     */
    public static <O> List<O> createObserverList() {
        return Lists.newArrayList();
    }

    /**
     * Instantiates a size-optimized multimap from keys to sets of values.
     * <p>
     * For a single key, many values can be associated according to the given bucket semantics.
     * <p>
     * 
     * @since 2.0
     */
    public static <K, V> IMultiLookup<K, V> createMultiLookup() {
        return new MultiLookup<>();
    }

}