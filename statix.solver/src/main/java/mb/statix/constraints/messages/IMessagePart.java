package mb.statix.constraints.messages;

import org.metaborg.util.functions.Action1;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;

public interface IMessagePart {

    String toString(TermFormatter formatter);

    void visitVars(Action1<ITermVar> onVar);

    IMessagePart apply(ISubstitution.Immutable subst);

    IMessagePart apply(IRenaming subst);

}