package mb.statix.scopegraph.reference;

interface DataWF<O> {

    boolean wf(O d);

    static <O> DataWF<O> ANY() {
        return new DataWF<O>() {

            public boolean wf(O d) {
                return true;
            }

        };
    }

}