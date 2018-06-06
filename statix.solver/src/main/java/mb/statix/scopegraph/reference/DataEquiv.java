package mb.statix.scopegraph.reference;

public interface DataEquiv<O> {

    boolean eq(O d1, O d2) throws ResolutionException, InterruptedException;

    boolean alwaysTrue();

    static <O> DataEquiv<O> ALL() {
        return new DataEquiv<O>() {

            public boolean eq(O d1, O d2) {
                return true;
            }

            public boolean alwaysTrue() {
                return true;
            }

        };
    }

    static <O> DataEquiv<O> NONE() {
        return new DataEquiv<O>() {

            public boolean eq(O d1, O d2) {
                return false;
            }

            public boolean alwaysTrue() {
                return false;
            }

        };
    }

}