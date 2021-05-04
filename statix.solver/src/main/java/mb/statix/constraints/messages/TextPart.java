package mb.statix.constraints.messages;

import java.io.Serializable;
import java.util.Objects;

import org.metaborg.util.functions.Action1;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;

public class TextPart implements IMessagePart, Serializable {
    private static final long serialVersionUID = 1L;

    private final String text;

    public TextPart(String text) {
        this.text = text;
    }

    @Override public String toString(@SuppressWarnings("unused") TermFormatter formatter) {
        return text;
    }

    @Override public void visitVars(@SuppressWarnings("unused") Action1<ITermVar> onVar) {
    }

    @Override public IMessagePart apply(@SuppressWarnings("unused") ISubstitution.Immutable subst) {
        return this;
    }

    @Override public IMessagePart apply(@SuppressWarnings("unused") IRenaming subst) {
        return this;
    }

    @Override public String toString() {
        return text;
    }

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        TextPart textPart = (TextPart) o;
        return Objects.equals(text, textPart.text);
    }

    @Override public int hashCode() {
        return Objects.hash(text);
    }
}
