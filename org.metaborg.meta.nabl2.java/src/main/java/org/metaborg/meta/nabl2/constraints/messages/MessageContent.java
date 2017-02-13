package org.metaborg.meta.nabl2.constraints.messages;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.spoofax.TermSimplifier;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;

public abstract class MessageContent implements IMessageContent {

    @Override public abstract MessageContent apply(Function1<ITerm, ITerm> f);

    @Override public IMessageContent withDefault(IMessageContent defaultContent) {
        return ImmutableCompoundMessage.of(Iterables2.from(this, of(" ("), defaultContent, of(")")));
    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    static abstract class TermMessage extends MessageContent {

        @Value.Parameter abstract ITerm getTerm();

        @Override public TermMessage apply(Function1<ITerm, ITerm> f) {
            return ImmutableTermMessage.of(f.apply(getTerm()));
        }

        @Override public String toString(String resource) {
            return TermSimplifier.focus(resource, getTerm()).toString();
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

        @Override public String toString(String resource) {
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

        @Override public String toString(String resource) {
            StringBuilder sb = new StringBuilder();
            getParts().stream().forEach(p -> sb.append(p.toString(resource)));
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

        @Override public String toString(String resource) {
            return toString();
        }

        @Override public String toString() {
            return "(no message)";
        }

    }

    public static IMatcher<MessageContent> matcher() {
        return M.<MessageContent>cases(
            // @formatter:off
            M.appl0("Default", t -> ImmutableDefaultMessage.of()),
            M.appl1("Formatted", M.listElems(partMatcher()), (t, ps) -> ImmutableCompoundMessage.of(ps)),
            partMatcher()
            // @formatter:on
        );
    }

    public static IMatcher<MessageContent> partMatcher() {
        return M.<MessageContent>cases(
            // @formatter:off
            M.appl1("Text", M.stringValue(), (t,s) -> ImmutableTextMessage.of(s)),
            M.appl1("Term", M.term(), (t,s) -> ImmutableTermMessage.of(s))
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