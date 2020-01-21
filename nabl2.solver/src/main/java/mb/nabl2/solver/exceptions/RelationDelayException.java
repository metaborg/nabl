package mb.nabl2.solver.exceptions;

public class RelationDelayException extends DelayException {

    private static final long serialVersionUID = 42L;

    private final String name;

    public RelationDelayException(String name) {
        this.name = name;
    }

    public String relation() {
        return name;
    }

}