package mb.statix.scopegraph.reference;

import java.util.List;

public interface DataLeq<V> {

    boolean leq(List<V> d1, List<V> d2) throws ResolutionException, InterruptedException;

    boolean alwaysTrue() throws InterruptedException;

    static <V> DataLeq<V> ALL() {
        return new DataLeq<V>() {

            public boolean leq(List<V> d1, List<V> d2) {
                return true;
            }

            public boolean alwaysTrue() {
                return true;
            }

        };
    }

    static <V> DataLeq<V> NONE() {
        return new DataLeq<V>() {

            public boolean leq(List<V> d1, List<V> d2) {
                return false;
            }

            public boolean alwaysTrue() {
                return false;
            }

        };
    }

}