package mb.statix.scopegraph.reference;

public interface DataWF<D> {

    boolean wf(D d) throws ResolutionException, InterruptedException;

    static <V> DataWF<V> ANY() {
        return new DataWF<V>() {

            @Override public boolean wf(V d) {
                return true;
            }

        };
    }

}