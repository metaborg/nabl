package mb.nabl2.terms.unification;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.util.TermFormatter;

public class UnifierFormatter implements TermFormatter {

    private final IUnifier unifier;
    private final int depth;

    public UnifierFormatter(IUnifier unifier, int depth) {
        this.unifier = unifier;
        this.depth = depth;
    }

    @Override public String format(ITerm term) {
        return unifier.toString(term, depth);
    }

}