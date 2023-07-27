/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package mb.nabl2.util.graph.alg.misc.memory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Map;

/**
 * A singleton immutable empty memory.
 * 
 * @author Gabor Bergmann
 * @since 2.0
 *
 */
public class EmptyMemory<T> implements IMemoryView<T> {

    @SuppressWarnings("rawtypes") private static final EmptyMemory INSTANCE = new EmptyMemory();

    @SuppressWarnings("unchecked") public static <T> EmptyMemory<T> instance() {
        return INSTANCE;
    }

    /**
     * Singleton; hidden constructor
     */
    private EmptyMemory() {
        super();
    }

    @Override public Iterator<T> iterator() {
        return Collections.<T>emptySet().iterator();
    }

    @Override public int getCount(T value) {
        return 0;
    }

    @Override public int getCountUnsafe(Object value) {
        return 0;
    }

    @Override public boolean containsNonZero(T value) {
        return false;
    }

    @Override public boolean containsNonZeroUnsafe(Object value) {
        return false;
    }

    @Override public Map.Immutable<T, Integer> asMap() {
        return CapsuleUtil.immutableMap();
    }

    @Override public int size() {
        return 0;
    }

    @Override public boolean isEmpty() {
        return true;
    }

    @Override public Set<T> distinctValues() {
        return Collections.emptySet();
    }

    @Override public Iterable<Entry<T, Integer>> entriesWithMultiplicities() {
        return Iterables2.empty();
    }

    @Override public int hashCode() {
        return IMemoryView.hashCode(this);
    }

    @Override public boolean equals(Object obj) {
        return IMemoryView.equals(this, obj);
    }

    @Override public String toString() {
        return "{}";
    }
}
