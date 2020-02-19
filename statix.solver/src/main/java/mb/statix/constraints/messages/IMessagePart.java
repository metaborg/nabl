package mb.statix.constraints.messages;

import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;

public interface IMessagePart {

    String toString(TermFormatter formatter);

    IMessagePart substitute(ISubstitution.Immutable subst);

}