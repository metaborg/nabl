package org.metaborg.meta.nabl2.scopegraph.terms;

import java.util.List;

import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.AbstractApplTerm;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;

import com.google.common.collect.ImmutableList;

@Value.Immutable
public abstract class Occurrence extends AbstractApplTerm implements IOccurrence, IApplTerm {

    private static final String OP = "Occurrence";

    // IOccurrence implementation

    @Value.Parameter @Override public abstract Namespace getNamespace();

    @Value.Parameter @Override public abstract ITerm getName();

    @Value.Parameter @Override public abstract TermIndex getPosition();

    // IApplTerm implementation

    @Value.Lazy @Override public String getOp() {
        return OP;
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        return ImmutableList.of(getNamespace(), getName(), getPosition());
    }

    public static IMatcher<Occurrence> matcher() {
        return M.appl3("Occurrence", Namespace.matcher(), M.term(), TermIndex.matcher(), (t, namespace, name,
                termIndex) -> {
            return ImmutableOccurrence.of(namespace, name, termIndex).setAttachments(t.getAttachments());
        });
    }

    // Object implementation

    @Override public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getNamespace().getName());
        sb.append("{");
        sb.append(getName());
        sb.append(" ");
        sb.append(getPosition());
        sb.append("}");
        return sb.toString();
    }

}