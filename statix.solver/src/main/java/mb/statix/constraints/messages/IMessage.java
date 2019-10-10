package mb.statix.constraints.messages;

import mb.nabl2.terms.substitution.ISubstitution;

public interface IMessage {

    IMessage apply(ISubstitution.Immutable subst);

}