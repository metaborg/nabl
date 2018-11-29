package mb.nabl2.terms.matching;

import mb.nabl2.terms.ITerm;

public class MismatchException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Pattern pattern;
    private final ITerm term;

    public MismatchException(Pattern pattern, ITerm term) {
        super("Cannot match " + term + " against " + pattern);
        this.pattern = pattern;
        this.term = term;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public ITerm getTerm() {
        return term;
    }

}