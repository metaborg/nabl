package mb.statix.scopegraph.reference;

public class IncompleteEdgeException extends ResolutionException {

    private static final long serialVersionUID = 1L;

    private final Object scope;
    private final Object label;

    public IncompleteEdgeException(Object scope, Object label) {
        super(scope + " incomplete in edge " + label);
        this.scope = scope;
        this.label = label;
    }

    @SuppressWarnings("unchecked") public <V> V scope() {
        return (V) scope;
    }

    @SuppressWarnings("unchecked") public <L> L label() {
        return (L) label;
    }

}