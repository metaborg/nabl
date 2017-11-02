package org.metaborg.meta.nabl2.terms.generic;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IBlobTerm;
import org.metaborg.meta.nabl2.terms.ITermVar;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMultiset;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class BlobTerm extends AbstractTerm implements IBlobTerm {

    @Value.Parameter @Override public abstract Object getValue();

    @Override public boolean isGround() {
        return true;
    }

    @Value.Default @Value.Auxiliary @Override public boolean isLocked() {
        return false;
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
        return Objects.hashCode(getValue());
    }

    @Override public boolean equals(Object other) {
        if(other == null) {
            return false;
        }
        if(!(other instanceof IBlobTerm)) {
            return false;
        }
        IBlobTerm that = (IBlobTerm) other;
        if(getValue().equals(that.getValue())) {
            return false;
        }
        return true;
    }

    @Override public String toString() {
        return getValue().toString();
    }

}