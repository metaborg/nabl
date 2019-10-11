package mb.statix.constraints.messages;

import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;

public class TextPart implements IMessagePart {

    private final String text;

    public TextPart(String text) {
        this.text = text;
    }

    @Override public String toString(TermFormatter formatter) {
        return text;
    }

    @Override public IMessagePart apply(ISubstitution.Immutable subst) {
        return this;
    }

}