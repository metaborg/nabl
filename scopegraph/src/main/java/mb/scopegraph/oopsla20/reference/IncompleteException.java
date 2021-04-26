package mb.scopegraph.oopsla20.reference;

public class IncompleteException extends ResolutionException {

    private static final long serialVersionUID = 1L;

    private final Object scope;
    private final EdgeOrData<? extends Object> label;

    public IncompleteException(Object scope, EdgeOrData<? extends Object> label) {
        super(scope + " incomplete in " + label);
        this.scope = scope;
        this.label = label;
    }

    @SuppressWarnings("unchecked") public <V> V scope() {
        return (V) scope;
    }

    @SuppressWarnings("unchecked") public <L> EdgeOrData<L> label() {
        return (EdgeOrData<L>) label;
    }

}