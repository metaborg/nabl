package mb.scopegraph.pepm16.terms;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.ImList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.build.AbstractApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.scopegraph.pepm16.INamespace;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ANamespace extends AbstractApplTerm implements INamespace, IApplTerm {

    private static final String OP1 = "Namespace";
    private static final String OP0 = "DefaultNamespace";

    // INamespace implementation

    @Value.Parameter @Override public abstract String getName();

    // IApplTerm implementation

    @Value.Lazy @Override public String getOp() {
        return getName().isEmpty() ? OP0 : OP1;
    }

    @Value.Lazy @Override public ImList.Immutable<ITerm> getArgs() {
        return getName().isEmpty() ? ImList.Immutable.of() : ImList.Immutable.of((ITerm) B.newString(getName()));
    }

    public static IMatcher<Namespace> matcher() {
        return M.preserveAttachments(M.cases(
        // @formatter:off
            M.appl0(OP0, (t) -> Namespace.of("")),
            M.appl1(OP1, M.stringValue(), (t, ns) -> Namespace.of(ns))
            // @formatter:on
        ));
    }

    @Override protected ANamespace check() {
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
        if(!(other instanceof Namespace)) {
            return super.equals(other);
        }
        final Namespace that = (Namespace) other;
        if(!getName().equals(that.getName())) {
            return false;
        }
        return true;
    }

    @Override public int hashCode() {
        return super.hashCode();
    }

    @Override public String toString() {
        return super.toString();
    }

}