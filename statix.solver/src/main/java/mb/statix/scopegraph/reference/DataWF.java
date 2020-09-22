package mb.statix.scopegraph.reference;

public interface DataWF<D> {

    boolean wf(D d) throws ResolutionException, InterruptedException;

    static <D> DataWF<D> ANY() {
        return new DataWF<D>() {

            @Override public boolean wf(D d) {
                return true;
            }

        };
    }

}