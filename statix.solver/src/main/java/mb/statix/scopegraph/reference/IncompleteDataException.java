package mb.statix.scopegraph.reference;

public class IncompleteDataException extends ResolutionException {

    private static final long serialVersionUID = 1L;

    private final Object scope;
    private final Object relation;

    public IncompleteDataException(Object scope, Object relation) {
        super(scope + " incomplete in data " + relation);
        this.scope = scope;
        this.relation = relation;
    }

    @SuppressWarnings("unchecked") public <V> V scope() {
        return (V) scope;
    }

    @SuppressWarnings("unchecked") public <L> L relation() {
        return (L) relation;
    }

}