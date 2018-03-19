package mb.nabl2.util.tuples;

import java.util.Objects;
import java.util.function.Predicate;

import mb.nabl2.scopegraph.ILabel;

/**
 * Marker interface for tuples that have a single label.
 */
public interface HasLabel<L extends ILabel> {

    L label();
    
    static Predicate<HasLabel<? extends ILabel>> labelEquals(ILabel label) {
        return tuple -> Objects.equals(tuple.label(), label);
    }
        
}