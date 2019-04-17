package mb.nabl2.scopegraph.terms;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

import mb.nabl2.scopegraph.INamespace;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.build.AbstractApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class Namespace extends AbstractApplTerm implements INamespace, IApplTerm {

    private static final String OP1 = "Namespace";
    private static final String OP0 = "DefaultNamespace";

    // INamespace implementation

    @Value.Parameter @Override public abstract String getName();

    // IApplTerm implementation

    @Value.Lazy @Override public String getOp() {
        return getName().isEmpty() ? OP0 : OP1;
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        return getName().isEmpty() ? ImmutableList.of() : ImmutableList.of((ITerm) B.newString(getName()));
    }

    public static IMatcher<Namespace> matcher() {
        return M.preserveAttachments(M.cases(
        // @formatter:off
            M.appl0(OP0, (t) -> ImmutableNamespace.of("")),
            M.appl1(OP1, M.stringValue(), (t, ns) -> ImmutableNamespace.of(ns))
            // @formatter:on
        ));
    }

    @Override protected Namespace check() {
        return this;
    }

    // Object implementation

    @Override public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override public int hashCode() {
        return super.hashCode();
    }

    @Override public String toString() {
        return super.toString();
    }

}