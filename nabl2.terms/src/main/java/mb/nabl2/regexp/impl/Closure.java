package mb.nabl2.regexp.impl;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.regexp.IRegExp;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class Closure<S> implements IRegExp<S> {

    @Value.Parameter public abstract IRegExp<S> getRE();

    @Override public <T> T match(IRegExp.ICases<S, T> visitor) {
        return visitor.closure(getRE());
    }

    @Override public String toString() {
        return "(" + getRE() + ")*";
    }

}