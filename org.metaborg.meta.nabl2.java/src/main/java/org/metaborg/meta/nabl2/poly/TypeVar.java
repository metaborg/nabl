package org.metaborg.meta.nabl2.poly;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.AbstractApplTerm;
import org.metaborg.meta.nabl2.terms.generic.TB;

import com.google.common.collect.ImmutableList;

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
        return ImmutableList.of(TB.newString(getName()));
    }

    public static IMatcher<TypeVar> matcher() {
        return M.appl1(OP, M.stringValue(), (t, name) -> {
            return ImmutableTypeVar.of(name).withAttachments(t.getAttachments());
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
        return "'" + getName();
    }

}