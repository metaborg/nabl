package mb.nabl2.terms.build;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableMultiset;

import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITermVar;

import java.util.Objects;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class StringTerm extends AbstractTerm implements IStringTerm {

    @Value.Parameter @Override public abstract String getValue();

    @Override public boolean isGround() {
        return true;
    }

    @Value.Lazy @Override public ImmutableMultiset<ITermVar> getVars() {
        return ImmutableMultiset.of();
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseString(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseString(this);
    }

    @Override public int hashCode() {
        return Objects.hash(
            getValue()
        );
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof IStringTerm
            && equals((IStringTerm)other, false);
    }

    public boolean equals(IStringTerm that, boolean compareAttachments) {
        if (this == that) return true;
        if (that == null) return false;
        if (this.hashCode() != that.hashCode()) return false;
        // @formatter:off
        return Objects.equals(this.getValue(), that.getValue())
            && (!compareAttachments || Objects.equals(this.getAttachments(), that.getAttachments()));
        // @formatter:on
    }

    @Override public String toString() {
        return "\"" + getValue() + "\"";
    }

}
