package mb.statix.scopegraph;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Objects;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.build.AbstractApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

@Value.Immutable(lazyhash = false)
@Serial.Version(value = 42L)
public abstract class AScope extends AbstractApplTerm implements IScope, IApplTerm, Comparable<IScope> {

    private static final String OP = "Scope";

    // IScope implementation

    @Value.Parameter @Override public abstract String getResource();

    @Value.Parameter @Override public abstract String getName();

    // IApplTerm implementation

    @Override public String getOp() {
        return OP;
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        return ImmutableList.of(B.newString(getResource()), B.newString(getName()));
    }

    public static IMatcher<Scope> matcher() {
        return M.preserveAttachments(M.appl2(OP, M.stringValue(), M.stringValue(), (t, resource, name) -> {
            if(t instanceof Scope) {
                return (Scope) t;
            } else {
                return Scope.of(resource, name);
            }
        }));
    }

    @Override protected AScope check() {
        return this;
    }

    // Object implementation

    @Override public int hashCode() {
        // We use the super-class hashcode to ensure that an AScope and an IApplTerm
        // with the same term representation have the same hash code.
        // super-class caches hashcode
        return super.hashCode();
    }

    @Override public boolean equals(Object other) {
        if(this == other)
            return true;
        if(!(other instanceof AScope))
            return super.equals(other);
        AScope that = (AScope) other;
        if(this.hashCode() != that.hashCode())
            return false;
        // @formatter:off
        return Objects.equals(this.getResource(), that.getResource())
            && Objects.equals(this.getName(), that.getName());
        // @formatter:on
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("#");
        if(!getResource().isEmpty()) {
            sb.append(getResource());
            sb.append("-");
        }
        sb.append(getName());
        return sb.toString();
    }

    @Override public int compareTo(final IScope scope) {
        int diffResource = getResource().compareTo(scope.getResource());
        if(diffResource != 0) {
            return diffResource;
        }

        return getName().compareTo(scope.getName());
    }

}
