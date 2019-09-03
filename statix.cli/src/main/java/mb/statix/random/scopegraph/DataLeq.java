package mb.statix.random.scopegraph;

import mb.statix.scopegraph.reference.ResolutionException;

public interface DataLeq<D> {

    boolean leq(D d1, D d2) throws ResolutionException, InterruptedException;

    boolean alwaysTrue() throws InterruptedException;

    static <V> DataLeq<V> ALL() {
        return new DataLeq<V>() {

            @Override public boolean leq(V d1, V d2) {
                return true;
            }

            @Override public boolean alwaysTrue() {
                return true;
            }

        };
    }

    static <V> DataLeq<V> NONE() {
        return new DataLeq<V>() {

            @Override public boolean leq(V d1, V d2) {
                return false;
            }

            @Override public boolean alwaysTrue() {
                return false;
            }

        };
    }

}