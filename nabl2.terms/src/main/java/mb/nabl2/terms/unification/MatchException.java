package mb.nabl2.terms.unification;

import mb.nabl2.terms.ITerm;

public class MatchException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ITerm pattern;
    private final ITerm term;

    public MatchException(ITerm pattern, ITerm term) {
        super("Cannot match " + term + " against " + pattern);
        this.pattern = pattern;
        this.term = term;
    }

    public ITerm getPattern() {
        return pattern;
    }

    public ITerm getTerm() {
        return term;
    }

}