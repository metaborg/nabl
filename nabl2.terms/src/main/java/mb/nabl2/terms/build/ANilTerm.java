package mb.nabl2.terms.build;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.INilTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

@Value.Immutable(builder = false, copy = true, prehash = false)
@Serial.Version(value = 42L)
abstract class ANilTerm extends AbstractTerm implements INilTerm {

    @Value.Derived @Override public int getMinSize() {
        return 0;
    }

    @Override public boolean isGround() {
        return true;
    }

    @Value.Lazy @Override public Set.Immutable<ITermVar> getVars() {
        return Set.Immutable.of();
    }

    @Override public INilTerm withAttachments(IAttachments value) {
        return (INilTerm) super.withAttachments(value);
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

    @Override public boolean equals(Object other) {
        if(this == other)
            return true;
        if(!(other instanceof INilTerm))
            return false;
        INilTerm that = (INilTerm) other;
        if(this.hashCode() != that.hashCode())
            return false;
        // @formatter:off
        return true;
        // @formatter:on
    }

    @Override public String toString() {
        return "[]";
    }

}
