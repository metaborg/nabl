package mb.nabl2.terms.unification;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier.Immutable;
import mb.nabl2.util.TermFormatter;

public class UnifierFormatter implements TermFormatter {

    private final IUnifier.Immutable unifier;
    private final int depth;

    public UnifierFormatter(Immutable unifier, int depth) {
        this.unifier = unifier;
        this.depth = depth;
    }

    public String format(ITerm term) {
        return unifier.toString(term, depth);
    }

    @Override public TermFormatter removeAll(Iterable<ITermVar> vars) {
        return new UnifierFormatter(unifier.removeAll(vars).unifier(), depth);
    }

}
