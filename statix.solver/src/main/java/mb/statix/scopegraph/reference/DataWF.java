package mb.statix.scopegraph.reference;

public interface DataWF<O> {

    boolean wf(O d) throws ResolutionException, InterruptedException;

    static <O> DataWF<O> ANY() {
        return new DataWF<O>() {

            public boolean wf(O d) {
                return true;
            }

        };
    }

}