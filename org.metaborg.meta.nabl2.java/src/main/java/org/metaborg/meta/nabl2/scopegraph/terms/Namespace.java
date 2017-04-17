package org.metaborg.meta.nabl2.scopegraph.terms;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.INamespace;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.AbstractApplTerm;
import org.metaborg.meta.nabl2.terms.generic.TB;

import com.google.common.collect.ImmutableList;

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
        return getName().isEmpty() ? ImmutableList.of() : ImmutableList.of((ITerm) TB.newString(getName()));
    }

    public static IMatcher<Namespace> matcher() {
        return M.cases(
            // @formatter:off
            M.appl0(OP0, (t) -> ImmutableNamespace.of("").withAttachments(t.getAttachments())),
            M.appl1(OP1, M.stringValue(), (t, ns) -> ImmutableNamespace.of(ns).withAttachments(t.getAttachments()))
            // @formatter:on
        );
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