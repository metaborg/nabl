package mb.statix.util;

import io.usethesource.capsule.Set;

public class Capsules {

    public static <T> Set.Immutable<T> newSet(Iterable<T> elements) {
        final Set.Transient<T> set = Set.Transient.of();
        elements.forEach(set::__insert);
        return set.freeze();
    }

}