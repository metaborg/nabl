package mb.nabl2.terms.unification;

import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.util.TermFormatter;

public class UnifierFormatter implements TermFormatter {

    private final IUnifier unifier;
    private final int depth;
    private final SpecializedTermFormatter specializedTermFormatter;

    public UnifierFormatter(IUnifier unifier, int depth) {
        this(unifier, depth, (t, u, f) -> Optional.empty());
    }

    public UnifierFormatter(IUnifier unifier, int depth, SpecializedTermFormatter specializedTermFormatter) {
        this.unifier = unifier;
        this.depth = depth;
        this.specializedTermFormatter = specializedTermFormatter;
    }

    @Override public String format(ITerm term) {
        return unifier.toString(term, depth, specializedTermFormatter);
    }

}