package mb.statix.terms;

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
import mb.statix.spoofax.StatixTerms;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class AOccurrence extends AbstractApplTerm implements IApplTerm {

    private static final String OP = "Occurrence";

    // IOccurrence implementation

    @Value.Parameter public abstract String getNamespace();

    @Value.Parameter public abstract List<ITerm> getName();

    @Value.Parameter public abstract ITerm getIndex();

    // IApplTerm implementation

    @Value.Lazy @Override public String getOp() {
        return OP;
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        return ImmutableList.of(B.newString(getNamespace()), B.newList(getName()), getIndex());
    }

    @Override protected Occurrence check() {
        return (Occurrence) this;
    }

    // Object implementation

    @Override public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override public int hashCode() {
        return super.hashCode();
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getNamespace());
        sb.append("{");
        sb.append(getName());
        sb.append(" ");
        sb.append(getIndex());
        sb.append("}");
        return sb.toString();
    }

    public static IMatcher<Occurrence> matcher(IMatcher<ITerm> term) {
        return M.appl3(OP, M.stringValue(), M.listElems(term), M.term(), (t, ns, name, idx) -> {
            return Occurrence.of(ns, name, idx);
        });
    }

}