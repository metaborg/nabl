package mb.nabl2.terms.stratego;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.IBlobTerm;
import mb.nabl2.terms.IConsTerm;
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.INilTerm;
import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.build.AbstractApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

@Value.Immutable(lazyhash = false)
@Serial.Version(value = 42L)
public abstract class ATermIndex extends AbstractApplTerm implements ITermIndex, IApplTerm {

    private static final String OP = "TermIndex";

    // ITermIndex implementation

    @Override @Value.Parameter public abstract String getResource();

    @Override @Value.Parameter public abstract int getId();

    @SuppressWarnings({ "unchecked" })
    public <T extends ITerm> T put(T term) {
        final IAttachments.Builder attachments = term.getAttachments().toBuilder();
        attachments.put(TermIndex.class, (TermIndex) this);
        return (T)term.withAttachments(attachments.build());
    }

    // IApplTerm implementation

    @Override public String getOp() {
        return OP;
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        return ImmutableList.of(B.newString(getResource()), B.newInt(getId()));
    }

    public static IMatcher<TermIndex> matcher() {
        return M.preserveAttachments(
                M.appl2(OP, M.stringValue(), M.integerValue(), (t, resource, id) -> TermIndex.of(resource, id)));
    }

    @Override protected TermIndex check() {
        return (TermIndex) this;
    }

    // Object implementation

    @Override public int hashCode() {
        // We use the super-class hashcode to ensure that a TermIndex and an IApplTerm
        // with the same term representation have the same hash code.
        return super.hashCode();
    }

    @Override public boolean equals(Object other) {
        if(this == other)
            return true;
        if(!(other instanceof TermIndex))
            return super.equals(other);
        TermIndex that = (TermIndex) other;
        if(this.hashCode() != that.hashCode())
            return false;
        // @formatter:off
        return Objects.equals(this.getResource(), that.getResource())
            && Objects.equals(this.getId(), that.getId());
        // @formatter:on
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("@");
        sb.append(getResource());
        sb.append(":");
        sb.append(getId());
        return sb.toString();
    }

    // static

    public static Optional<TermIndex> get(ITerm term) {
        return get(term.getAttachments());
    }

    public static Optional<TermIndex> get(IAttachments attachments) {
        return Optional.ofNullable(attachments.get(TermIndex.class));
    }

    /**
     * Finds the first indexed subterm term, in pre-order.
     *
     * @param term
     *            The term to find the index of.
     * @return The {@link TermIndex} of the first eligible term.
     */
    public static Optional<TermIndex> find(ITerm term) {
        return get(term.getAttachments()).map(Optional::of).orElseGet(() -> {
            switch(term.termTag()) {
                case IApplTerm: { IApplTerm appl = (IApplTerm) term;
                    return find(appl.getArgs().iterator());
                }

                case IConsTerm: { IConsTerm cons = (IConsTerm) term;
                    return find(cons.getHead()).map(Optional::of).orElseGet(() -> find(cons.getTail()));
                }

                case INilTerm:
                case IStringTerm:
                case IIntTerm:
                case IBlobTerm:
                case ITermVar: {
                    return Optional.empty();
                }
            }
            // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
            throw new RuntimeException("Missing case for ITerm subclass/tag");
        });
    }

    private static Optional<TermIndex> find(Iterator<ITerm> termIterator) {
        if(!termIterator.hasNext()) {
            return Optional.empty();
        }
        final ITerm term = termIterator.next();
        return find(term).<Optional<TermIndex>>map(Optional::of).orElseGet(() -> find(termIterator));
    }

    public static boolean has(ITerm term) {
        return get(term).isPresent();
    }

    public static <T extends ITerm> T copy(ITerm src, T dst) {
        //noinspection unchecked
        return (T)get(src).map(o -> o.put(dst)).orElse(dst);
    }

    public static TermIndex of(String resource, int id) {
        return TermIndex.of(resource, id);
    }

}
