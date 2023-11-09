package mb.nabl2.constraints.messages;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.function.Function;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

public abstract class MessageContent implements IMessageContent {

    private static final String TERM = "Term";
    private static final String TEXT = "Text";
    private static final String DEFAULT = "Default";
    private static final String FORMATTED = "Formatted";

    @Override public abstract MessageContent apply(Function1<ITerm, ITerm> f);

    @Override public IMessageContent withDefault(IMessageContent defaultContent) {
        return this;
    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    static abstract class ATermMessage extends MessageContent {

        @Value.Parameter abstract ITerm getTerm();

        @Override public ATermMessage apply(Function1<ITerm, ITerm> f) {
            return TermMessage.of(f.apply(getTerm()));
        }

        @Override public ITerm build() {
            return B.newAppl(TERM, getTerm());
        }

        @Override public String toString(Function<ITerm, String> pp) {
            return pp.apply(getTerm());
        }

        @Override public String toString() {
            return getTerm().toString();
        }

    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    static abstract class ATextMessage extends MessageContent {

        @Value.Parameter abstract String getText();

        @Override public ATextMessage apply(Function1<ITerm, ITerm> f) {
            return this;
        }

        @Override public ITerm build() {
            return B.newAppl(TEXT, B.newString(getText()));
        }

        @Override public String toString(Function<ITerm, String> pp) {
            return getText();
        }

        @Override public String toString() {
            return getText();
        }

    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    static abstract class ACompoundMessage extends MessageContent {

        @Value.Parameter abstract ImList.Immutable<IMessageContent> getParts();

        @Override public ACompoundMessage apply(Function1<ITerm, ITerm> f) {
            return CompoundMessage
                    .of(getParts().stream().map(p -> p.apply(f)).collect(ImList.Immutable.toImmutableList()));
        }

        @Override public IMessageContent withDefault(IMessageContent defaultContent) {
            return CompoundMessage.of(getParts().stream().map(p -> p.withDefault(defaultContent))
                    .collect(ImList.Immutable.toImmutableList()));
        }

        @Override public ITerm build() {
            List<ITerm> parts =
                    getParts().stream().map(IMessageContent::build).collect(ImList.Immutable.toImmutableList());
            return B.newAppl(FORMATTED, (ITerm) B.newList(parts));
        }

        @Override public String toString(Function<ITerm, String> pp) {
            StringBuilder sb = new StringBuilder();
            getParts().stream().forEach(p -> sb.append(p.toString(pp)));
            return sb.toString();
        }

        @Override public String toString() {
            StringBuilder sb = new StringBuilder();
            getParts().stream().forEach(p -> sb.append(p.toString()));
            return sb.toString();
        }

    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    static abstract class ADefaultMessage extends MessageContent {

        @Override public ADefaultMessage apply(Function1<ITerm, ITerm> f) {
            return this;
        }

        @Override public IMessageContent withDefault(IMessageContent defaultContent) {
            return defaultContent;
        }

        @Override public ITerm build() {
            return B.newAppl(DEFAULT);
        }

        @Override public String toString(Function<ITerm, String> pp) {
            return toString();
        }

        @Override public String toString() {
            return "(no message)";
        }

    }

    public static IMatcher<IMessageContent> matcher() {
        // @formatter:off
        return M.<IMessageContent>cases(
            M.appl0(DEFAULT, (t) -> DefaultMessage.of()),
            M.appl1(FORMATTED, M.listElems(partMatcher()), (t, ps) -> CompoundMessage.of(ps)),
            M.string(s -> TextMessage.of(s.getValue())),
            partMatcher(),
            M.term(t -> CompoundMessage.of(ImList.Immutable.of(
                TermMessage.of(t),
                TextMessage.of(" (error message was malformed)")
            )))
        );
        // @formatter:on
    }

    public static IMatcher<IMessageContent> partMatcher() {
        // @formatter:off
        return M.<IMessageContent>cases(
            M.appl1(TEXT, M.stringValue(), (t,s) -> TextMessage.of(s)),
            M.appl1(TERM, M.term(), (t,s) -> TermMessage.of(s))
        );
        // @formatter:on
    }

    public static MessageContent of(String text) {
        return TextMessage.of(text);
    }

    public static MessageContent of() {
        return DefaultMessage.of();
    }

    public static class Builder {

        private final ImList.Mutable<IMessageContent> parts;

        private Builder() {
            this.parts = ImList.Mutable.of();
        }

        public Builder append(String text) {
            parts.add(TextMessage.of(text));
            return this;
        }

        public Builder append(ITerm term) {
            parts.add(TermMessage.of(term));
            return this;
        }

        public Builder append(IMessageContent content) {
            parts.add(content);
            return this;
        }

        public MessageContent build() {
            return CompoundMessage.of(parts.freeze());
        }

    }

    public static Builder builder() {
        return new Builder();
    }

}