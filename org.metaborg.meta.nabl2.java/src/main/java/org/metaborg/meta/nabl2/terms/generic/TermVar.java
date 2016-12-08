package org.metaborg.meta.nabl2.terms.generic;

import java.util.Iterator;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;

import com.google.common.collect.ImmutableClassToInstanceMap;

@Value.Immutable
@Serial.Structural
public abstract class TermVar implements ITermVar {

    @Value.Parameter @Override public abstract String getResource();

    @Value.Parameter @Override public abstract String getName();

    public boolean isGround() {
        return false;
    }

    @Override public int getLength() {
        throw new IllegalStateException();
    }

    @Override public Iterator<ITerm> iterator() {
        throw new IllegalStateException();
    }

    @Override public ImmutableClassToInstanceMap<Object> getAttachments() {
        return ImmutableClassToInstanceMap.<Object> builder().build();
    }

    @Override public <T> T match(ITerm.Cases<T> cases) {
        return cases.caseVar(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(ITerm.CheckedCases<T,E> cases) throws E {
        return cases.caseVar(this);
    }

    @Override public <T> T match(IListTerm.Cases<T> cases) {
        return cases.caseVar(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IListTerm.CheckedCases<T,E> cases) throws E {
        return cases.caseVar(this);
    }

    @Override public String toString() {
        return "?" + getResource() + "-" + getName();
    }

}