package mb.nabl2.terms.build;

import java.util.Objects;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.ITermVar;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class AIntTerm extends AbstractTerm implements IIntTerm {

    @Value.Parameter @Override public abstract int getValue();

    @Override public boolean isGround() {
        return true;
    }

    @Value.Lazy @Override public Set.Immutable<ITermVar> getVars() {
        return Set.Immutable.of();
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseInt(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseInt(this);
    }

    @Override public int hashCode() {
        return Objects.hash(getValue());
    }

    @Override public boolean equals(Object other) {
        if(this == other)
            return true;
        if(!(other instanceof IIntTerm))
            return false;
        IIntTerm that = (IIntTerm) other;
        if(this.hashCode() != that.hashCode())
            return false;
        // @formatter:off
        return Objects.equals(this.getValue(), that.getValue());
        // @formatter:on
    }

    @Override public String toString() {
        return Integer.toString(getValue());
    }

}
