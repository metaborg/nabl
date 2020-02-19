package mb.statix.constraints.messages;

import mb.nabl2.terms.substitution.ISubstitutable;
import mb.nabl2.util.TermFormatter;

public interface IMessagePart extends ISubstitutable<IMessagePart> {

    String toString(TermFormatter formatter);

}