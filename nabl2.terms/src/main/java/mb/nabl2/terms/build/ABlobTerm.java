package mb.nabl2.terms.build;

import java.util.Objects;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.IBlobTerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.CapsuleUtil;

@Value.Immutable(builder = true, copy = true, prehash = false)
@Serial.Version(value = 42L)
abstract class ABlobTerm extends AbstractTerm implements IBlobTerm {

    @Value.Parameter @Override public abstract Object getValue();

    @Override public boolean isGround() {
        return true;
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return CapsuleUtil.immutableSet();
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseBlob(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseBlob(this);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(getValue());
            hashCode = result;
        }
        return result;
    }

    @Override public boolean equals(Object other) {
        if(this == other)
            return true;
        if(!(other instanceof IBlobTerm))
            return false;
        IBlobTerm that = (IBlobTerm) other;
        if(this.hashCode() != that.hashCode())
            return false;
        // @formatter:off
        return Objects.equals(this.getValue(), that.getValue());
        // @formatter:on
    }

    @Override public String toString() {
        return getValue().toString();
    }

}
