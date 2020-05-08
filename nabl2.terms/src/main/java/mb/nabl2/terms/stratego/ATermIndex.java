package mb.nabl2.terms.stratego;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.build.AbstractApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ATermIndex extends AbstractApplTerm implements ITermIndex, IApplTerm {

    private static final String OP = "TermIndex";

    // ITermIndex implementation

    @Override @Value.Parameter public abstract String getResource();

    @Override @Value.Parameter public abstract int getId();

    @SuppressWarnings({ "unchecked", "rawtypes" }) public ITerm put(ITerm term) {
        final ImmutableClassToInstanceMap.Builder<Object> attachments = ImmutableClassToInstanceMap.builder();
        // builder does not allow overwriting entries, so we need to filter out
        // the term origin, in case it is already there
        // @formatter:off
        term.getAttachments().entrySet().stream()
            .filter(e -> !TermIndex.class.equals(e.getKey()))
            .forEach(e -> {
                attachments.put((Class)e.getKey(), e.getValue());
            });
        // @formatter:on
        attachments.put(TermIndex.class, (TermIndex) this);
        return term.withAttachments(attachments.build());
    }

    // IApplTerm implementation

    @Override public String getOp() {
        return OP;
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        return ImmutableList.of(B.newString(getResource()), B.newInt(getId()));
    }

    public static IMatcher<TermIndex> matcher() {
        return M.preserveAttachments(M.appl2(OP, M.stringValue(), M.integerValue(),
                (t, resource, id) -> TermIndex.of(resource, id)));
    }

    @Override protected TermIndex check() {
        return (TermIndex) this;
    }

    @Override public abstract TermIndex withAttachments(ImmutableClassToInstanceMap<Object> value);

    // Object implementation

    @Override public int hashCode() {
        // We use the super-class hashcode to ensure that a TermIndex and an IApplTerm
        // with the same term representation have the same hash code.
        return super.hashCode();
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TermIndex)) return super.equals(other);
        TermIndex that = (TermIndex)other;
        if (this.hashCode() != that.hashCode()) return false;
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

    public static Optional<TermIndex> get(ClassToInstanceMap<Object> attachments) {
        return Optional.ofNullable(attachments.getInstance(TermIndex.class));
    }

    public static ITerm copy(ITerm src, ITerm dst) {
        return get(src).map(o -> o.put(dst)).orElse(dst);
    }

    public static TermIndex of(String resource, int id) {
        return TermIndex.of(resource, id);
    }

}
