package mb.statix.constraints.messages;

import java.io.Serializable;

import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;

public class TextPart implements IMessagePart, Serializable {
    private static final long serialVersionUID = 1L;

    private final String text;

    public TextPart(String text) {
        this.text = text;
    }

    @Override public String toString(TermFormatter formatter) {
        return text;
    }

    @Override public IMessagePart substitute(ISubstitution.Immutable subst) {
        return this;
    }

    @Override public String toString() {
        return text;
    }

}