package mb.nabl2.terms.build;

import mb.nabl2.terms.ITerm;
import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableMultiset;

import mb.nabl2.terms.IBlobTerm;
import mb.nabl2.terms.ITermVar;
import java.util.Objects;

@Value.Immutable(builder = false, copy = true, prehash = false)
@Serial.Version(value = 42L)
abstract class BlobTerm extends AbstractTerm implements IBlobTerm {

    @Value.Parameter @Override public abstract Object getValue();

    @Override public boolean isGround() {
        return true;
    }

    @Value.Lazy @Override public ImmutableMultiset<ITermVar> getVars() {
        return ImmutableMultiset.of();
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseBlob(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseBlob(this);
    }

    @Override public int hashCode() {
        return Objects.hash(
            getValue()
        );
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof IBlobTerm)) return false;
        IBlobTerm that = (IBlobTerm)other;
        if (this.hashCode() != that.hashCode()) return false;
        // @formatter:off
        return Objects.equals(this.getValue(), that.getValue());
        // @formatter:on
    }

    @Override public String toString() {
        return getValue().toString();
    }

}
