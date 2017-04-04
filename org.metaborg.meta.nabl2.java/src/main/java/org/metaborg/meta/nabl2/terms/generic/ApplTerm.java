package org.metaborg.meta.nabl2.terms.generic;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;

import com.google.common.collect.Iterables;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class ApplTerm extends AbstractApplTerm implements IApplTerm {

    @Value.Parameter @Override public abstract String getOp();

    @Value.Parameter @Override public abstract List<ITerm> getArgs();

    @Value.Lazy @Override public int getArity() {
        return Iterables.size(getArgs());
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseAppl(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseAppl(this);
    }

    @Override public int hashCode() {
        return super.hashCode();
    }

    @Override public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override public String toString() {
        return super.toString();
    }

}