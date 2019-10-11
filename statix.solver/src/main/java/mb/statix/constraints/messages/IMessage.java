package mb.statix.constraints.messages;

import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;

public interface IMessage {

    MessageKind kind();

    Optional<ITerm> origin();

    IMessage apply(ISubstitution.Immutable subst);

}