package mb.nabl2.stratego;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;

import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.terms.ITerm;
import static mb.nabl2.terms.matching.Transform.T;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.scopegraph.pepm16.terms.Occurrence;
import mb.scopegraph.pepm16.terms.Scope;

public class MessageTerms {

    public static ITerm toTerms(Iterable<? extends IMessageInfo> messages, IUnifier unifier) {
        return B.newList(
                Iterables2.stream(messages).map(mi -> toTerm(mi, unifier)).collect(ImmutableList.toImmutableList()));
    }

    public static ITerm toTerm(IMessageInfo message, IUnifier unifier) {
        return B.newTuple(message.getOriginTerm(), B.newString(message.getContent().apply(unifier::findRecursive)
                .apply(MessageTerms::specializeForToString).toString()));
    }

    // replace special terms such as scopes and occurrences with a blob with their
    // string version, to get nicer printed terms
    private static ITerm specializeForToString(ITerm t) {
        // @formatter:off
        return T.sometd(M.<ITerm>cases(
            Scope.matcher().map(s -> B.newBlob(s.toString())),
            Occurrence.matcher().map(o -> B.newBlob(o.toString()))
        )::match).apply(t);
        // @formatter:on
    }

}