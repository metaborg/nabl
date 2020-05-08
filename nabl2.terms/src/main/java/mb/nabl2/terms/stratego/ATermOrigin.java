package mb.nabl2.terms.stratego;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.attachments.OriginAttachment;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;

import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ATermOrigin {

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

    @SuppressWarnings({ "unchecked", "rawtypes" }) public ITerm put(ITerm term) {
        final ImmutableClassToInstanceMap.Builder<Object> attachments = ImmutableClassToInstanceMap.builder();
        // builder does not allow overwriting entries, so we need to filter out
        // the term origin, in case it is already there
        // @formatter:off
        term.getAttachments().entrySet().stream()
            .filter(e -> !TermOrigin.class.equals(e.getKey()))
            .forEach(e -> {
                attachments.put((Class)e.getKey(), e.getValue());
            });
        // @formatter:on
        attachments.put(TermOrigin.class, (TermOrigin) this);
        return term.withAttachments(attachments.build());
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
        return TermOrigin.of(ImploderAttachment.createCompactPositionAttachment(resource, 0, 0, 0, 0));
    }

    public static Optional<TermOrigin> get(ITerm term) {
        return get(term.getAttachments());
    }

    public static ITerm copy(ITerm src, ITerm dst) {
        return get(src).map(o -> o.put(dst)).orElse(dst);
    }

    public static Optional<TermOrigin> get(ClassToInstanceMap<Object> attachments) {
        return Optional.ofNullable(attachments.getInstance(TermOrigin.class));
    }

    // Stratego term interaction

    public static Optional<TermOrigin> get(IStrategoTerm term) {
        return Optional.ofNullable(ImploderAttachment.get(OriginAttachment.tryGetOrigin(term)))
                .map(ia -> TermOrigin.of(ia));
    }

    public static void copy(IStrategoTerm src, IStrategoTerm dst) {
        get(src).ifPresent(o -> o.put(dst));
    }

    public void put(IStrategoTerm term) {
        term.putAttachment(getImploderAttachment());
    }

}