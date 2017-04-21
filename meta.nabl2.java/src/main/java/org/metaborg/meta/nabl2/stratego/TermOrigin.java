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

    @Value.Parameter public abstract String getResource();


    @Value.Parameter public abstract int getStartOffset();

    @Value.Parameter public abstract int getStartLine();

    @Value.Parameter public abstract int getStartColumn();


    @Value.Parameter public abstract int getEndOffset();

    @Value.Parameter public abstract int getEndLine();

    @Value.Parameter public abstract int getEndColumn();


    public int getLine() {
        return getStartLine();
    }

    public int getColumn() {
        return getStartColumn();
    }


    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("@");
        sb.append(getResource());
        sb.append(":");
        sb.append(getLine());
        sb.append(",");
        sb.append(getColumn());
        return sb.toString();
    }

    public static Optional<TermOrigin> get(ITerm term) {
        return get(term.getAttachments());
    }

    public static Optional<TermOrigin> get(ClassToInstanceMap<Object> attachments) {
        return Optional.ofNullable(attachments.getInstance(TermOrigin.class));
    }

    // ImploderAttachment interaction

    public static TermOrigin fromImploderAttachment(ImploderAttachment attachment) {
        final IToken left = attachment.getLeftToken();
        final IToken right = attachment.getRightToken();
        final String resource = left.getFilename();
        return ImmutableTermOrigin.of(resource,
            left.getStartOffset(), left.getLine(), left.getColumn(),
            right.getEndOffset(), right.getEndLine(), right.getEndColumn());
    }

    public ImploderAttachment toImploderAttachment() {
        return ImploderAttachment.createCompactPositionAttachment(getResource(), getLine(), getColumn(),
            getStartOffset(), getEndOffset());
    }

    public static Optional<ImploderAttachment> getImploderAttachment(IStrategoTerm term) {
        return Optional.ofNullable(ImploderAttachment.get(OriginAttachment.tryGetOrigin(term)));
    }

}