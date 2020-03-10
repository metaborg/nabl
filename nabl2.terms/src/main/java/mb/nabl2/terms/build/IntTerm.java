package mb.nabl2.terms.build;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableMultiset;

import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.ITermVar;

import java.util.Objects;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class IntTerm extends AbstractTerm implements IIntTerm {

    @Value.Parameter @Override public abstract int getValue();

    @Override public boolean isGround() {
        return true;
    }

    @Value.Lazy @Override public ImmutableMultiset<ITermVar> getVars() {
        return ImmutableMultiset.of();
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseInt(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseInt(this);
    }

    @Override public int hashCode() {
        return Objects.hash(
            getValue()
        );
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof IIntTerm
            && equals((IIntTerm)other, false);
    }

    public boolean equals(IIntTerm that, boolean compareAttachments) {
        if (this == that) return true;
        if (that == null) return false;
        if (this.hashCode() != that.hashCode()) return false;
        // @formatter:off
        return Objects.equals(this.getValue(), that.getValue())
            && (!compareAttachments || Objects.equals(this.getAttachments(), that.getAttachments()));
        // @formatter:on
    }

    @Override public String toString() {
        return Integer.toString(getValue());
    }

}
