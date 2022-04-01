package mb.nabl2.terms.build;

import java.util.Objects;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.IBlobTerm;
import mb.nabl2.terms.ITermVar;

@Value.Immutable(lazyhash = false)
@Serial.Version(value = 42L)
abstract class ABlobTerm extends AbstractTerm implements IBlobTerm {

    @Value.Parameter @Override public abstract Object getValue();

    @Override public boolean isGround() {
        return true;
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return CapsuleUtil.immutableSet();
    }

    @Override public void visitVars(@SuppressWarnings("unused") Action1<ITermVar> onVar) {
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseBlob(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseBlob(this);
    }

    @Override public int hashCode() {
        return Objects.hash(getValue());
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
