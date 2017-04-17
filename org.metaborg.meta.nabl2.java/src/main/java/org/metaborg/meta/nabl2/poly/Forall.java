package org.metaborg.meta.nabl2.poly;

import java.util.List;
import java.util.Set;

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
public abstract class Forall extends AbstractApplTerm implements IApplTerm {

    private static final String OP = "Forall";

    // IOccurrence implementation

    @Value.Parameter public abstract Set<TypeVar> getTypeVars();

    @Value.Parameter public abstract ITerm getType();

    // IApplTerm implementation

    @Value.Lazy @Override public String getOp() {
        return OP;
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        ITerm vars = TB.newList(getTypeVars());
        return ImmutableList.of(vars, getType());
    }

    public static IMatcher<Forall> matcher() {
        return M.appl2(OP, M.listElems(TypeVar.matcher()), M.term(), (t, vars, type) -> {
            return ImmutableForall.of(vars, type).withAttachments(t.getAttachments());
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
        sb.append("forall");
        sb.append(getTypeVars());
        sb.append(".");
        sb.append(getType());
        return sb.toString();
    }

}