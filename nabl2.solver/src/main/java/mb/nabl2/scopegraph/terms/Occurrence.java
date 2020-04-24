package mb.nabl2.scopegraph.terms;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.build.AbstractApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.stratego.TermOrigin;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class Occurrence extends AbstractApplTerm implements IOccurrence, IApplTerm, Comparable<IOccurrence> {

    private static final String OP = "Occurrence";

    // IOccurrence implementation

    @Value.Parameter @Override public abstract Namespace getNamespace();

    @Value.Parameter @Override public abstract ITerm getName();

    @Value.Parameter @Override public abstract OccurrenceIndex getIndex();

    @Override public ITerm getNameOrIndexOrigin() {
        return TermOrigin.get(getName()).map(o -> getName()).orElseGet(this::getIndex);
    }

    @Value.Lazy @Override public SpacedName getSpacedName() {
        return ImmutableSpacedName.of(getNamespace(), getName());
    }

    // IApplTerm implementation

    @Value.Lazy @Override public String getOp() {
        return OP;
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        return ImmutableList.of(getNamespace(), getName(), getIndex());
    }

    public static IMatcher<Occurrence> matcher() {
        return M.preserveAttachments(M.appl3("Occurrence", Namespace.matcher(), M.ground(M.term()),
                OccurrenceIndex.matcher(), (t, namespace, name, index) -> {
                    return ImmutableOccurrence.of(namespace, name, index);
                }));
    }

    @Override protected Occurrence check() {
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
        if(!(other instanceof Occurrence)) {
            return super.equals(other);
        }
        final Occurrence that = (Occurrence) other;
        if(!getNamespace().equals(that.getNamespace())) {
            return false;
        }
        if(!getName().equals(that.getName())) {
            return false;
        }
        if(!getIndex().equals(that.getIndex())) {
            return false;
        }
        return true;
    }

    @Override public int hashCode() {
        return super.hashCode();
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getNamespace().getName());
        sb.append("{");
        sb.append(getName());
        sb.append(" ");
        sb.append(getIndex());
        sb.append("}");
        return sb.toString();
    }

    @Override public int compareTo(final IOccurrence other) {
        int diffNamespace = getNamespace().getName().compareTo(other.getNamespace().getName());
        if(diffNamespace != 0) {
            return diffNamespace;
        }

        int diffName = getName().toString().compareTo(other.getName().toString());
        if(diffName != 0) {
            return diffName;
        }

        int diffIndex = getIndex().toString().compareTo(other.getIndex().toString());
        return diffIndex;
    }

}