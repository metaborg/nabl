package mb.nabl2.terms.build;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableMultiset;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.INilTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

import java.util.Objects;

@Value.Immutable(builder = false, copy = true, prehash = false)
@Serial.Version(value = 42L)
abstract class NilTerm extends AbstractTerm implements INilTerm {

    @Override public boolean isGround() {
        return true;
    }

    @Value.Lazy @Override public ImmutableMultiset<ITermVar> getVars() {
        return ImmutableMultiset.of();
    }

    @Override public <T> T match(ITerm.Cases<T> cases) {
        return cases.caseList(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(ITerm.CheckedCases<T, E> cases) throws E {
        return cases.caseList(this);
    }

    @Override public <T> T match(IListTerm.Cases<T> cases) {
        return cases.caseNil(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IListTerm.CheckedCases<T, E> cases) throws E {
        return cases.caseNil(this);
    }

    @Override public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof INilTerm
            && equals((INilTerm)other, false);
    }

    public boolean equals(INilTerm that, boolean compareAttachments) {
        if (this == that) return true;
        if (that == null) return false;
        if (this.hashCode() != that.hashCode()) return false;
        // @formatter:off
        return (!compareAttachments || Objects.equals(this.getAttachments(), that.getAttachments()));
        // @formatter:on
    }

    @Override public String toString() {
        return "[]";
    }

}
