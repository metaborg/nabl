package mb.statix.scopegraph.reference;

class EpsilonWF<L> implements LabelWF<L> {

    private final boolean stepped;

    public EpsilonWF(boolean stepped) {
        this.stepped = stepped;
    }

    public LabelWF<L> step(L l) {
        return new EpsilonWF<>(true);
    }

    public boolean wf() {
        return !stepped;
    }

    public boolean empty() {
        return stepped;
    }

}