package mb.nabl2.terms.build;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.ITermVar;

@Value.Immutable(lazyhash = false)
@Serial.Version(value = 42L)
abstract class AIntTerm extends AbstractTerm implements IIntTerm {

    @Value.Parameter @Override public abstract int getValue();

    @Override public boolean isGround() {
        return true;
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return CapsuleUtil.immutableSet();
    }

    @Override public void visitVars(@SuppressWarnings("unused") Action1<ITermVar> onVar) {
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseInt(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseInt(this);
    }


    @Override public int hashCode() {
        return getValue();
    }

    @Override public boolean equals(Object other) {
        if(this == other)
            return true;
        if(!(other instanceof IIntTerm))
            return false;
        final IIntTerm that = (IIntTerm) other;
        return this.getValue() == that.getValue();
    }

    @Override public String toString() {
        return Integer.toString(getValue());
    }

}
