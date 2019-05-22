package mb.nabl2.constraints.messages;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.stratego.TermIndex;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class MessageInfo implements IMessageInfo {

    private static final String OP = "Message";

    @Value.Parameter @Override public abstract MessageKind getKind();

    @Value.Parameter @Override public abstract IMessageContent getContent();

    @Value.Parameter @Override public abstract ITerm getOriginTerm();

    @Override public IMessageInfo withDefaultContent(IMessageContent defaultContent) {
        return ImmutableMessageInfo.of(getKind(), getContent().withDefault(defaultContent), getOriginTerm());
    }

    @Override public IMessageInfo apply(Function1<ITerm, ITerm> f) {
        return this.withContent(getContent().apply(f));
    }

    public static IMatcher<MessageInfo> matcher() {
        return M.appl3(OP, MessageKind.matcher(), MessageContent.matcher(), M.term(), (appl, kind, message, origin) -> {
            return ImmutableMessageInfo.of(kind, message, origin);
        });
    }

    public static IMatcher<MessageInfo> matcherOnlyOriginTerm() {
        return M.term(MessageInfo::of);
    }

    public static ITerm build(IMessageInfo messageInfo) {
        return B.newAppl(OP, MessageKind.build(messageInfo.getKind()), messageInfo.getContent().build(),
                messageInfo.getOriginTerm());
    }

    public static ITerm buildOnlyOriginTerm(IMessageInfo messageInfo) {
        return messageInfo.getOriginTerm();
    }

    public static IMatcher<MessageInfo> matcherEditorMessage(MessageKind kind) {
        return M.tuple2(M.term(), MessageContent.matcher(), (t, origin, message) -> {
            return ImmutableMessageInfo.of(kind, message, origin);
        });
    }

    public static MessageInfo of(ITerm originTerm) {
        return ImmutableMessageInfo.of(MessageKind.ERROR, MessageContent.of(), originTerm);
    }

    public static MessageInfo empty() {
        return ImmutableMessageInfo.of(MessageKind.ERROR, MessageContent.of(), B.EMPTY_TUPLE);
    }

    @Override public String toString() {
        return getKind().name().toLowerCase() + " " + getContent().toString() + " " + TermIndex.get(getOriginTerm());
    }

}