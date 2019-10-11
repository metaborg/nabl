package mb.statix.constraints.messages;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;

public class TermPart implements IMessagePart {

    private final ITerm term;

    public TermPart(ITerm term) {
        this.term = term;
    }

    @Override public String toString(TermFormatter formatter) {
        return formatter.format(term);
    }

    @Override public IMessagePart apply(ISubstitution.Immutable subst) {
        return new TermPart(subst.apply(term));
    }

}