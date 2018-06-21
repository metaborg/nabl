package mb.statix.scopegraph.reference;

import java.util.List;

public interface DataEquiv<V> {

    boolean eq(List<V> d1, List<V> d2) throws ResolutionException, InterruptedException;

    boolean alwaysTrue() throws InterruptedException;

    static <V> DataEquiv<V> ALL() {
        return new DataEquiv<V>() {

            public boolean eq(List<V> d1, List<V> d2) {
                return true;
            }

            public boolean alwaysTrue() {
                return true;
            }

        };
    }

    static <V> DataEquiv<V> NONE() {
        return new DataEquiv<V>() {

            public boolean eq(List<V> d1, List<V> d2) {
                return false;
            }

            public boolean alwaysTrue() {
                return false;
            }

        };
    }

}