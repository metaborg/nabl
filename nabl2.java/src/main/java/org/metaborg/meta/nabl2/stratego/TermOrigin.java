package org.metaborg.meta.nabl2.stratego;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.attachments.OriginAttachment;

import com.google.common.collect.ClassToInstanceMap;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class TermOrigin {

    @Value.Parameter public abstract ImploderAttachment getImploderAttachment();

    public String getResource() {
        return getImploderAttachment().getLeftToken().getFilename();
    }

    public IToken getLeftToken() {
        return getImploderAttachment().getLeftToken();
    }

    public IToken getRightToken() {
        return getImploderAttachment().getRightToken();
    }

    @Override public String toString() {
        IToken token = getImploderAttachment().getLeftToken();
        StringBuilder sb = new StringBuilder();
        sb.append("@");
        sb.append(token.getFilename());
        sb.append(":");
        sb.append(token.getLine());
        sb.append(",");
        sb.append(token.getColumn());
        return sb.toString();
    }

    public static TermOrigin of(String resource) {
        return ImmutableTermOrigin.of(ImploderAttachment.createCompactPositionAttachment(resource, 0, 0, 0, 0));
    }

    public static Optional<TermOrigin> get(ITerm term) {
        return get(term.getAttachments());
    }

    public static Optional<TermOrigin> get(ClassToInstanceMap<Object> attachments) {
        return Optional.ofNullable(attachments.getInstance(TermOrigin.class));
    }

    // Stratego term interaction

    public static Optional<TermOrigin> get(IStrategoTerm term) {
        return Optional.ofNullable(ImploderAttachment.get(OriginAttachment.tryGetOrigin(term)))
                .map(ia -> ImmutableTermOrigin.of(ia));
    }

    public void put(IStrategoTerm term) {
        term.putAttachment(getImploderAttachment());
    }

}