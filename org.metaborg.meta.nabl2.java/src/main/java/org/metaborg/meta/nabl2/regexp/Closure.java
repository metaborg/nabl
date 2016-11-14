package org.metaborg.meta.nabl2.regexp;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable
@Serial.Structural
abstract class Closure<S> implements IRegExp<S> {

    public abstract IRegExp<S> getRE();

    public abstract IRegExpBuilder<S> getBuilder();

    @Override public boolean isNullable() {
        return true;
    }

    @Override public <T> T accept(IRegExpFunction<S,T> visitor) {
        return visitor.closure(getRE());
    }

    @Override public String toString() {
        return "(" + getRE() + ")*";
    }

}