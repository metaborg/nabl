package mb.nabl2.util.tuples;

import java.util.Objects;
import java.util.function.Predicate;

import mb.nabl2.scopegraph.IScope;

/**
 * Marker interface for tuples that have a single scope.
 */
public interface HasScope<S extends IScope> {

    S scope();

    static Predicate<HasScope<? extends IScope>> scopeEquals(IScope scope) {
        return tuple -> Objects.equals(tuple.scope(), scope);
    }

}