package mb.statix.scopegraph.reference;

public interface LabelWF<L> {

    LabelWF<L> step(L l);

    boolean wf();

    boolean empty();

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

    static <L> LabelWF<L> EPSILON() {
        return new EpsilonWF<>(false);
    }

}