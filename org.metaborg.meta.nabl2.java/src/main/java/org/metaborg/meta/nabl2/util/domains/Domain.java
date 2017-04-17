package org.metaborg.meta.nabl2.util.domains;

import java.util.Arrays;
import java.util.Set;

import com.google.common.collect.Sets;

public class Domain<E> {

    private final Set<E> elements;
    private final boolean complement;

    private Domain(Set<E> elements, boolean complement) {
        this.elements = elements;
        this.complement = complement;
    }

    public Domain<E> complement() {
        return new Domain<>(elements, !complement);
    }

    public boolean contains(E t) {
        return elements.contains(t) ^ complement;
    }

    public boolean isEmpty() {
        return !complement && elements.isEmpty();
    }

    public boolean addAll(@SuppressWarnings("unchecked") E... elements) {
        return this.elements.addAll(Arrays.asList(elements));
    }

    @SafeVarargs public static <E> Domain<E> of(E... elements) {
        return of(Arrays.asList(elements));
    }

    public static <E> Domain<E> of(Iterable<E> elements) {
        return new Domain<>(Sets.newHashSet(elements), false);
    }

    public static <E> Domain<E> intersection(Domain<E> d1, Domain<E> d2) {
        if(!d1.complement) { // d1 == { ... }
            if(!d2.complement) { // d2 == { ... }
                return Domain.of(Sets.intersection(d1.elements, d2.elements));
            } else { // d2 == D \ { ... }
                return Domain.of(Sets.difference(d1.elements, d2.elements));
            }
        } else { // d1 == D \ { ... }
            if(!d2.complement) { // d2 == { ... }
                return Domain.of(Sets.difference(d2.elements, d1.elements));
            } else { // d2 == D \ { ... }
                return Domain.of(Sets.union(d1.elements, d2.elements)).complement();
            }
        }

    }

    public static <E> Domain<E> union(Domain<E> d1, Domain<E> d2) {
        if(!d1.complement) { // d1 == { ... }
            if(!d2.complement) { // d2 == { ... }
                return Domain.of(Sets.union(d1.elements, d2.elements));
            } else { // d2 == D \ { ... }
                return Domain.of(Sets.difference(d2.elements, d1.elements)).complement();
            }
        } else { // d1 == D \ { ... }
            if(!d2.complement) { // d2 == { ... }
                return Domain.of(Sets.difference(d1.elements, d2.elements)).complement();
            } else { // d2 == D \ { ... }
                return Domain.of(Sets.intersection(d1.elements, d2.elements)).complement();
            }
        }

    }

    public static <E> Domain<E> difference(Domain<E> d1, Domain<E> d2) {
        if(!d1.complement) { // d1 == { ... }
            if(!d2.complement) { // d2 == { ... }
                return Domain.of(Sets.difference(d1.elements, d2.elements));
            } else { // d2 == D \ { ... }
                return Domain.of(Sets.intersection(d1.elements, d2.elements));
            }
        } else { // d1 == D \ { ... }
            if(!d2.complement) { // d2 == { ... }
                return Domain.of(Sets.union(d1.elements, d2.elements)).complement();
            } else { // d2 == D \ { ... }
                return Domain.of(Sets.difference(d2.elements, d1.elements));
            }
        }
    }

    public static <E> boolean isSubset(Domain<E> d1, Domain<E> d2) {
        return difference(d1, d2).isEmpty();
    }

}