package mb.statix.constraints.messages;

import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;

public interface IMessage {

    MessageKind kind();

    String toString(TermFormatter formatter);

    Optional<ITerm> origin();

    IMessage substitute(ISubstitution.Immutable subst);

//    default IMessage substitute(ISubstitution.Immutable subst) {
//        return apply(subst, PersistentRenaming.Immutable.of());
//    }

//    default IMessage apply(IRenaming.Immutable renaming) {
//        return apply(PersistentSubstitution.Immutable.of(), renaming);
//    }

}