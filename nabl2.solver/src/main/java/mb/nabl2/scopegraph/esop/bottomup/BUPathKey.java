package mb.nabl2.scopegraph.esop.bottomup;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.terms.SpacedName;

/**
 * Key used in paths sets. It is a tuple of name and label, but without structural comparison. Correctness requires that
 * the same object is used everywhere, which is ensured in {@link BUNameResolution#pathKey(SpacedName, L)}.
 */
class BUPathKey<L extends ILabel> {

    private final SpacedName name;
    private final L label;

    BUPathKey(SpacedName name, L label) {
        this.name = name;
        this.label = label;
    }

    SpacedName name() {
        return name;
    }

    L label() {
        return label;
    }

    volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(hashCode == 0) {
            result = System.identityHashCode(this);
            hashCode = result;
        }
        return result;
    }

    @Override public boolean equals(Object obj) {
        return this == obj;
    }

}