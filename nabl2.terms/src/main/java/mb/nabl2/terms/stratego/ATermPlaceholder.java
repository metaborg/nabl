package mb.nabl2.terms.stratego;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.ITerm;

/**
 * A term attachment for placeholders.
 */
@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ATermPlaceholder {

    @Value.Parameter public abstract String getName();

    @SuppressWarnings({ "unchecked", "rawtypes" }) public <T extends ITerm> T put(T term) {
        final IAttachments.Builder attachments = term.getAttachments().toBuilder();
        attachments.put(TermPlaceholder.class, (TermPlaceholder) this);
        return (T)term.withAttachments(attachments.build());
    }

    @Override public String toString() {
        return getName();
    }
    // static

    public static Optional<TermPlaceholder> get(ITerm term) {
        return get(term.getAttachments());
    }

    public static Optional<TermPlaceholder> get(IAttachments attachments) {
        return Optional.ofNullable(attachments.get(TermPlaceholder.class));
    }

    public static boolean has(ITerm term) {
        return get(term).isPresent();
    }

    public static <T extends ITerm> T copy(ITerm src, T dst) {
        //noinspection unchecked
        return (T)get(src).map(o -> o.put(dst)).orElse(dst);
    }

    public static TermPlaceholder of(String name) {
        return TermPlaceholder.of(name);
    }


}
