package mb.statix.scopegraph.reference;

public interface LabelWF<L> {

    LabelWF<L> step(L l);

    /**
     * Returns if the current path is well-formed.
     */
    boolean wf() throws ResolutionException, InterruptedException;

    /**
     * Returns if none of the paths with the current prefix are well-formed.
     */
    boolean empty() throws ResolutionException, InterruptedException;

    static <L> LabelWF<L> ANY() {
        return new LabelWF<L>() {

            public LabelWF<L> step(L l) {
                return this;
            }

            public boolean wf() {
                return true;
            }

            public boolean empty() {
                return false;
            }

        };
    }

}