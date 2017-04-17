package org.metaborg.meta.nabl2.terms.generic;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IStringTerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class StringTerm extends AbstractTerm implements IStringTerm {

    @Value.Parameter @Override public abstract String getValue();

    @Override public boolean isGround() {
        return true;
    }

    @Value.Lazy @Override public PSet<ITermVar> getVars() {
        return HashTreePSet.empty();
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseString(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseString(this);
    }

    @Override public int hashCode() {
        return getValue().hashCode();
    }

    @Override public boolean equals(Object other) {
        if(other == null) {
            return false;
        }
        if(!(other instanceof IStringTerm)) {
            return false;
        }
        IStringTerm that = (IStringTerm) other;
        if(!getValue().equals(that.getValue())) {
            return false;
        }
        return true;
    }

    @Override public String toString() {
        return "\"" + getValue() + "\"";
    }

}