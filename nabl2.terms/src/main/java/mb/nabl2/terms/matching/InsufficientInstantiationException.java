package mb.nabl2.terms.matching;

import mb.nabl2.terms.ITermVar;

public class InsufficientInstantiationException extends Exception {

    private static final long serialVersionUID = 1L;

    public InsufficientInstantiationException(Pattern pattern, ITermVar... vars) {
        super("Insufficiently instantiated " + vars + " to match against " + pattern);
    }

}