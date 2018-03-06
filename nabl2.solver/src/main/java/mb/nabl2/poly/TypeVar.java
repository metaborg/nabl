package mb.nabl2.poly;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.build.AbstractApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class TypeVar extends AbstractApplTerm implements IApplTerm {

    private static final String OP = "TVar";

    // IOccurrence implementation

    @Value.Parameter public abstract String getName();

    // IApplTerm implementation

    @Value.Lazy @Override public String getOp() {
        return OP;
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        return ImmutableList.of(B.newString(getName()));
    }

    public static IMatcher<TypeVar> matcher() {
        return M.preserveAttachments(M.appl1(OP, M.stringValue(), (t, name) -> {
            return ImmutableTypeVar.of(name);
        }));
    }

    @Override protected TypeVar check() {
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
        return "'" + getName();
    }

}