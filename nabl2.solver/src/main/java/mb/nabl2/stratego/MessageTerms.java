package mb.nabl2.stratego;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;

import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.terms.ITerm;

public class MessageTerms {

    public static ITerm toTerms(Iterable<? extends IMessageInfo> messages) {
        return B.newList(Iterables2.stream(messages).map(MessageTerms::toTerm).collect(Collectors.toList()));
    }

    public static ITerm toTerm(IMessageInfo message) {
        return B.newTuple(message.getOriginTerm(), B.newString(message.getContent().toString()));
    }

}