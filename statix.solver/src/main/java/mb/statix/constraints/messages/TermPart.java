package mb.statix.constraints.messages;

import java.io.Serializable;
import java.util.Objects;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;

public class TermPart implements IMessagePart, Serializable {
    private static final long serialVersionUID = 1L;

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

    @Override public IMessagePart apply(IRenaming subst) {
        return new TermPart(subst.apply(term));
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(term);
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        TermPart termPart = (TermPart)o;
        return Objects.equals(term, termPart.term);
    }

    @Override
    public int hashCode() {
        return Objects.hash(term);
    }
}
