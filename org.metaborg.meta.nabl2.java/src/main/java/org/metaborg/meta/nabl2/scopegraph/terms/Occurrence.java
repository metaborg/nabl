package org.metaborg.meta.nabl2.scopegraph.terms;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.AbstractApplTerm;

import com.google.common.collect.ImmutableList;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class Occurrence extends AbstractApplTerm implements IOccurrence, IApplTerm {

    private static final String OP = "Occurrence";

    // IOccurrence implementation

    @Value.Parameter @Override public abstract Namespace getNamespace();

    @Value.Parameter @Override public abstract ITerm getName();

    @Value.Parameter @Override public abstract OccurrenceIndex getIndex();

    // IApplTerm implementation

    @Value.Lazy @Override public String getOp() {
        return OP;
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        return ImmutableList.of(getNamespace(), getName(), getIndex());
    }

    public static IMatcher<Occurrence> matcher() {
        return M.appl3("Occurrence", Namespace.matcher(), M.term(), OccurrenceIndex.matcher(), (t, namespace, name, index) -> {
            return ImmutableOccurrence.of(namespace, name, index).withAttachments(t.getAttachments());
        });
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
        sb.append(getNamespace().getName());
        sb.append("{");
        sb.append(getName());
        sb.append(" ");
        sb.append(getIndex());
        sb.append("}");
        return sb.toString();
    }

}