package mb.nabl2.terms.matching;

import mb.nabl2.terms.ITermVar;

public class InsufficientInstantiationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ITermVar var;

    public InsufficientInstantiationException(ITermVar var) {
        this.var = var;
    }

    public ITermVar getVar() {
        return var;
    }

    @Override public String getMessage() {
        final StringBuilder sb = new StringBuilder();
        sb.append(var).append(" insufficiently instantiated");
        return sb.toString();
    }

}