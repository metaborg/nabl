package mb.scopegraph.schema;

public abstract class Bound {

    private static final Bound INFINITE = new Infinite();

    public static Bound finite(int n) {
        return new Finite(n);
    }

    public static Bound infinite() {
        return INFINITE;
    }

    public abstract boolean lte(int n);

    public abstract boolean gte(int n);

    private static class Finite extends Bound {

        private final int value;

        public Finite(int value) {
            this.value = value;
        }

        @Override public boolean lte(int n) {
            return n <= value;
        }

        @Override public boolean gte(int n) {
            return n >= value;
        }

    }

    private static class Infinite extends Bound {

        @Override public boolean lte(int n) {
            return true;
        }

        @Override public boolean gte(int n) {
            return false;
        }

    }

}
