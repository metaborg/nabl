/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package mb.nabl2.util.graph.alg.misc.memory;

import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

import io.usethesource.capsule.Map;

/**
 * A read-only view on a memory containing a positive or negative number of equal() copies for some values. During
 * iterations, each distinct value is iterated only once.
 * 
 * <p>
 * See {@link IMemory}.
 * 
 * <p>
 * Implementors must provide semantic (not identity-based) hashCode() and equals() using the static helpers
 * {@link #hashCode(IMemoryView)} and {@link #equals(IMemoryView, Object)} here.
 * 
 * @author Gabor Bergmann
 *
 * @since 2.0
 */
public interface IMemoryView<T> extends Iterable<T> {

    /**
     * Returns the number of occurrences of the given value.
     * 
     * @return the number of occurrences
     */
    int getCount(T value);

    /**
     * Returns the number of occurrences of the given value (which may be of any type).
     * 
     * @return the number of occurrences
     */
    int getCountUnsafe(Object value);

    /**
     * @return true if the given value is contained with a nonzero multiplicity
     */
    boolean containsNonZero(T value);

    /**
     * @return true if the given value (which may be of any type) is contained with a nonzero multiplicity
     */
    boolean containsNonZeroUnsafe(Object value);

    /**
     * @return the number of distinct values
     */
    int size();

    /**
     * 
     * @return iff contains at least one value with non-zero occurrences
     */
    boolean isEmpty();

    /**
     * The set of distinct values
     */
    Set<T> distinctValues();

    /**
     * @return an unmodifiable view of contained values with their multiplicities
     */
    Iterable<Entry<T, Integer>> entriesWithMultiplicities();

    /**
     * Process contained values with their multiplicities
     */
    default void forEachEntryWithMultiplicities(BiConsumer<T, Integer> entryConsumer) {
        for(Entry<T, Integer> e : entriesWithMultiplicities()) {
            entryConsumer.accept(e.getKey(), e.getValue());
        }
    }


    /**
     * For compatibility with legacy code relying on element-to-integer maps.
     * 
     * @return an unmodifiable view of contained values with their multiplicities
     */
    public Map.Immutable<T, Integer> asMap();

    /**
     * For compatibility with legacy code relying on element-to-integer maps.
     * 
     * @return an unmodifiable view of contained values with their multiplicities
     */
    public static <T> IMemoryView<T> fromMap(Map.Immutable<T, Integer> wrapped) {
        return new MapBackedMemoryView<>(wrapped);
    }

    /**
     * Provides semantic equality comparison.
     */
    public static <T> boolean equals(IMemoryView<T> self, Object obj) {
        if(obj instanceof IMemoryView<?>) {
            IMemoryView<?> other = (IMemoryView<?>) obj;
            if(other.size() != self.size())
                return false;
            for(Entry<?, Integer> entry : other.entriesWithMultiplicities()) {
                if(!entry.getValue().equals(self.getCountUnsafe(entry.getKey())))
                    return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Provides semantic hashCode() comparison.
     */
    public static <T> int hashCode(IMemoryView<T> memory) {
        int hashCode = 0;
        for(T value : memory.distinctValues()) {
            hashCode += value.hashCode() ^ Integer.hashCode(memory.getCount(value));
        }
        return hashCode;
    }
}