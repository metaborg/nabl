package org.metaborg.meta.nabl2.constraints.messages;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;

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
    static abstract class TermMessage extends MessageContent {

        @Value.Parameter abstract ITerm getTerm();

        @Override public TermMessage apply(Function1<ITerm, ITerm> f) {
            return ImmutableTermMessage.of(f.apply(getTerm()));
        }

        @Override public ITerm build() {
            return TB.newAppl(TERM, getTerm());
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
    static abstract class TextMessage extends MessageContent {

        @Value.Parameter abstract String getText();

        @Override public TextMessage apply(Function1<ITerm, ITerm> f) {
            return this;
        }

        @Override public ITerm build() {
            return TB.newAppl(TEXT, TB.newString(getText()));
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
    static abstract class CompoundMessage extends MessageContent {

        @Value.Parameter abstract List<IMessageContent> getParts();

        @Override public CompoundMessage apply(Function1<ITerm, ITerm> f) {
            return ImmutableCompoundMessage.of(getParts().stream().map(p -> p.apply(f)).collect(Collectors.toList()));
        }

        @Override public IMessageContent withDefault(IMessageContent defaultContent) {
            return ImmutableCompoundMessage
                    .of(getParts().stream().map(p -> p.withDefault(defaultContent)).collect(Collectors.toList()));
        }

        @Override public ITerm build() {
            List<ITerm> parts = getParts().stream().map(IMessageContent::build).collect(Collectors.toList());
            return TB.newAppl(FORMATTED, (ITerm) TB.newList(parts));
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
    static abstract class DefaultMessage extends MessageContent {

        @Override public DefaultMessage apply(Function1<ITerm, ITerm> f) {
            return this;
        }

        @Override public IMessageContent withDefault(IMessageContent defaultContent) {
            return defaultContent;
        }

        @Override public ITerm build() {
            return TB.newAppl(DEFAULT);
        }

        @Override public String toString(Function<ITerm, String> pp) {
            return toString();
        }

        @Override public String toString() {
            return "(no message)";
        }

    }

    public static IMatcher<MessageContent> matcher() {
        return M.<MessageContent>cases(
            // @formatter:off
            M.appl0(DEFAULT, (t) -> ImmutableDefaultMessage.of()),
            M.appl1(FORMATTED, M.listElems(partMatcher()), (t, ps) -> ImmutableCompoundMessage.of(ps)),
            M.string(s -> ImmutableTextMessage.of(s.getValue())),
            partMatcher(),
            M.term(t -> ImmutableCompoundMessage.of(Iterables2.from(
                ImmutableTermMessage.of(t),
                ImmutableTextMessage.of(" (error message was malformed)")
            )))
            // @formatter:on
        );
    }

    public static IMatcher<MessageContent> partMatcher() {
        return M.<MessageContent>cases(
            // @formatter:off
            M.appl1(TEXT, M.stringValue(), (t,s) -> ImmutableTextMessage.of(s)),
            M.appl1(TERM, M.term(), (t,s) -> ImmutableTermMessage.of(s))
            // @formatter:on
        );
    }

    public static MessageContent of(String text) {
        return ImmutableTextMessage.of(text);
    }

    public static MessageContent of() {
        return ImmutableDefaultMessage.of();
    }

    public static class Builder {

        private final ImmutableList.Builder<IMessageContent> parts;

        private Builder() {
            this.parts = ImmutableList.builder();
        }

        public Builder append(String text) {
            parts.add(ImmutableTextMessage.of(text));
            return this;
        }

        public Builder append(ITerm term) {
            parts.add(ImmutableTermMessage.of(term));
            return this;
        }

        public Builder append(IMessageContent content) {
            parts.add(content);
            return this;
        }

        public MessageContent build() {
            return ImmutableCompoundMessage.of(parts.build());
        }

    }

    public static Builder builder() {
        return new Builder();
    }

}