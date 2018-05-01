package mb.nabl2.stratego;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;

import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;

public class MessageTerms {

    public static ITerm toTerms(Iterable<? extends IMessageInfo> messages, IUnifier unifier) {
        return B.newList(Iterables2.stream(messages).map(mi -> toTerm(mi, unifier)).collect(Collectors.toList()));
    }

    public static ITerm toTerm(IMessageInfo message, IUnifier unifier) {
        return B.newTuple(message.getOriginTerm(),
                B.newString(message.getContent().apply(unifier::findRecursive).toString()));
    }

}