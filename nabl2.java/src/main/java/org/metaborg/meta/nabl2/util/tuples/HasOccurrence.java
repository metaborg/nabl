package org.metaborg.meta.nabl2.util.tuples;

import java.util.Objects;
import java.util.function.Predicate;

import org.metaborg.meta.nabl2.scopegraph.IOccurrence;

/**
 * Marker interface for tuples that have a single occurrence.
 */
public interface HasOccurrence<O extends IOccurrence> {

    O occurrence();
    
    static Predicate<HasOccurrence<? extends IOccurrence>> occurrenceEquals(IOccurrence occurrence) {
        return tuple -> Objects.equals(tuple.occurrence(), occurrence);
    }
        
}