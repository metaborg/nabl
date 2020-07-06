package mb.nabl2.scopegraph.terms;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

import mb.nabl2.scopegraph.IScope;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.build.AbstractApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

@Value.Immutable
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
        return M.preserveAttachments(M.appl2("Scope", M.stringValue(), M.stringValue(),
                (t, resource, name) -> Scope.of(resource, name)));
    }

    @Override protected AScope check() {
        return this;
    }

    // Object implementation

    @Override public boolean equals(Object other) {
        if(other == null) {
            return false;
        }
        if(other == this) {
            return true;
        }
        if(!(other instanceof Scope)) {
            return super.equals(other);
        }
        final Scope that = (Scope) other;
        if(!getResource().equals(that.getResource())) {
            return false;
        }
        if(!getName().equals(that.getName())) {
            return false;
        }
        return true;
    }

    @Override public int hashCode() {
        return super.hashCode();
    }

    @Override public String toString() {
        return "#" + getResource() + "-" + getName();
    }

    @Override public int compareTo(final IScope scope) {
        int diffResource = getResource().compareTo(scope.getResource());
        if(diffResource != 0) {
            return diffResource;
        }

        int diffName = getName().toString().compareTo(scope.getName());
        return diffName;
    }

}