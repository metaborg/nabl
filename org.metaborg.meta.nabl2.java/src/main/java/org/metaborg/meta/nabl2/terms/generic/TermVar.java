package org.metaborg.meta.nabl2.terms.generic;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IAnnotation;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;

import com.google.common.collect.ImmutableClassToInstanceMap;

@Value.Immutable
@Serial.Structural
public abstract class TermVar implements ITerm, ITermVar {

    public abstract String getResource();

    public abstract String getName();

    public boolean isGround() {
        return false;
    }

    @Value.Auxiliary @Override public abstract ImmutableClassToInstanceMap<IAnnotation> getAnnotations();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseVar(this);
    }

    @Override public <T, E extends Throwable> T matchThrows(CheckedCases<T,E> cases) throws E {
        return cases.caseVar(this);
    }

    @Override public String toString() {
        return "?" + getResource() + "-" + getName();
    }

}