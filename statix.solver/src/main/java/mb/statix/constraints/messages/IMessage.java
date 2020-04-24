package mb.statix.constraints.messages;

import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;

public interface IMessage {

    MessageKind kind();

    String toString(TermFormatter formatter);

    Optional<ITerm> origin();

    IMessage apply(ISubstitution.Immutable subst);

    IMessage apply(IRenaming subst);

}