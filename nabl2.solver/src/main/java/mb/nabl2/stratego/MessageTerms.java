package mb.nabl2.stratego;

import static mb.nabl2.terms.build.TermBuild.B;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;

import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.u.IUnifier;

public class MessageTerms {

    public static ITerm toTerms(Iterable<? extends IMessageInfo> messages, IUnifier unifier) {
        return B.newList(
                Iterables2.stream(messages).map(mi -> toTerm(mi, unifier)).collect(ImmutableList.toImmutableList()));
    }

    public static ITerm toTerm(IMessageInfo message, IUnifier unifier) {
        return B.newTuple(message.getOriginTerm(),
                B.newString(message.getContent().apply(unifier::findRecursive).toString()));
    }

}