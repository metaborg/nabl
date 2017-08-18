package meta.flowspec.java.lattice;

import org.pcollections.Empty;
import org.pcollections.PSet;

public class SetLattice<E> implements CompleteLattice<PSet<E>> {
    private final PSet<E> top;

    /**
     * @param set The set to consider the top of this lattice
     */
    public SetLattice(PSet<E> set) {
        this.top = set;
    }

    @Override
    public PSet<E> top() {
        return top;
    }

    @Override
    public PSet<E> bottom() {
        return Empty.set();
    }

    @Override
    public boolean lte(PSet<E> one, PSet<E> other) {
        return other.containsAll(one); // one isSubsetOf other
    }

    @Override
    public PSet<E> glb(PSet<E> one, PSet<E> other) {
        return one.minusAll(one.minusAll(other));
    }

    @Override
    public PSet<E> lub(PSet<E> one, PSet<E> other) {
        return one.plusAll(other);
    }
}
