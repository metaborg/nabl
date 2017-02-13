package org.metaborg.meta.nabl2.constraints.messages;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class MessageInfo implements IMessageInfo {

    @Value.Parameter @Override public abstract MessageKind getKind();

    @Value.Parameter @Override public abstract IMessageContent getContent();

    @Value.Parameter @Override public abstract ITerm getOriginTerm();

    @Override public IMessageInfo withDefault(IMessageContent defaultContent) {
        return ImmutableMessageInfo.of(getKind(), getContent().withDefault(defaultContent), getOriginTerm());
    }

    public static IMatcher<MessageInfo> matcher() {
        return M.appl3("Message", MessageKind.matcher(), MessageContent.matcher(), M.term(),
            (appl, kind, message, origin) -> {
                return ImmutableMessageInfo.of(kind, message, origin);
            });
    }

    public static IMatcher<MessageInfo> matcherOnlyOriginTerm() {
        return M.term(MessageInfo::of);
    }

    public static MessageInfo of(ITerm originTerm) {
        return ImmutableMessageInfo.of(MessageKind.ERROR, MessageContent.of(), originTerm);
    }

    @Override public String toString() {
        return getKind().name().toLowerCase() + " " + getContent().toString() + " " + TermIndex.get(getOriginTerm());
    }

    /*
    
    @Override public UnsatisfiableException makeException(String defaultMessage, Iterable<ITerm> contextTerms,
            IUnifier unifier) {
        Iterable<ITerm> programPoints = getOrigin().map(t -> Iterables2.singleton(t)).orElse(contextTerms);
        String message = getMessage().flatMap(m -> formatMessage().match(unifier.find(m))).orElse("") + ". "
                + defaultMessage;
        return new UnsatisfiableException(getKind(), message, programPoints);
    }

    private IMatcher<String> formatMessage() {
        return M.cases(
            // @formatter:off
            M.appl1("Formatted", M.listElems(formatMessagePart()), (t, ps) -> {
                StringBuilder sb = new StringBuilder();
                for(String s : ps) {
                    sb.append(s);
                }
                return sb.toString();
            }),
            M.term(t -> t.toString())
            // @formatter:on
        );
    }

    private IMatcher<String> formatMessagePart() {
        return M.<String> cases(
            // @formatter:off
            M.appl1("Text", M.stringValue(), (t,s) -> s),
            M.appl1("Term", M.term(), (t,s) -> TermSimplifier.simplify(s).toString()),
            M.term(t -> TermSimplifier.simplify(t).toString())
            // @formatter:on
        );
    }

    public static MessageInfo of(ITerm term) {
        return ImmutableMessageInfo.of(Kind.ERROR, Optional.empty(), Optional.of(term));
    }

    public static IMatcher<MessageInfo> simpleMatcher() {
        return M.term(MessageInfo::of);
    }

    public static IMatcher<MessageInfo> matcher() {
        return M.appl3("Message", kind(), message(), origin(), (appl, kind, message, origin) -> {
            return ImmutableMessageInfo.of(kind, message, origin);
        });
    }

    private static IMatcher<Optional<ITerm>> message() {
        return M.cases(
            // @formatter:off
            M.appl0("Default", (t) -> {
                return Optional.empty();
            }),
            M.term((t) -> {
                return Optional.of(t);
            })
            // @formatter:on
        );
    }

    private static IMatcher<Optional<ITerm>> origin() {
        return M.cases(
            // @formatter:off
            M.appl0("NAME", (t) -> {
                return Optional.empty();
            }),
            M.term((t) -> {
                return Optional.of(t);
            })
            // @formatter:on
        );
    }

    private static IMatcher<Kind> kind() {
        return M.cases(
            // @formatter:off
            M.appl0("Error", e -> Kind.ERROR),
            M.appl0("Warning", e -> Kind.WARNING),
            M.appl0("Note", e -> Kind.NOTE)
            // @formatter:on
        );
    }

*/

}