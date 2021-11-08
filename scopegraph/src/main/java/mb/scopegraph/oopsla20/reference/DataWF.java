package mb.scopegraph.oopsla20.reference;

@FunctionalInterface
public interface DataWF<D> {

    boolean wf(D d) throws ResolutionException, InterruptedException;

    static <D> DataWF<D> ANY() {
        return new DataWF<D>() {

            @Override public boolean wf(@SuppressWarnings("unused") D d) {
                return true;
            }

        };
    }

}