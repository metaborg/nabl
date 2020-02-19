package mb.statix.constraints.messages;

import java.io.Serializable;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

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

    @Override public String toString(TermFormatter formatter) {
        return text;
    }

    @Override public Set<ITermVar> boundVars() {
        return ImmutableSet.of();
    }

    @Override public Set<ITermVar> freeVars() {
        return ImmutableSet.of();
    }

    @Override public IMessagePart doSubstitute(IRenaming.Immutable localRenaming, ISubstitution.Immutable totalSubst) {
        return this;
    }

    @Override public String toString() {
        return text;
    }

}