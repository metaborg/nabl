package mb.nabl2.terms.stratego;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.build.AbstractApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

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
        // @formatter:off
        return get(term.getAttachments()).map(Optional::of).orElseGet(() ->
            term.match(Terms.<Optional<TermIndex>>cases()
                .appl(appl -> find(appl.getArgs().iterator()))
                .list(list -> list.match(ListTerms.<Optional<TermIndex>>cases()
                    .cons(cons -> find(cons.getHead()).map(Optional::of).orElseGet(() -> find(cons.getTail())))
                    .otherwise(__ -> Optional.empty())))
                .otherwise(__ -> Optional.empty()))
        );
        // @formatter:on
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
