package mb.scopegraph.regexp.impl;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.scopegraph.regexp.IRegExp;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class AConcat<S> implements IRegExp<S> {

    @Value.Parameter public abstract IRegExp<S> getLeft();

    @Value.Parameter public abstract IRegExp<S> getRight();

    @Override public <T> T match(IRegExp.ICases<S, T> visitor) {
        return visitor.concat(getLeft(), getRight());
    }

    @Override public String toString() {
        return "(" + getLeft() + " " + getRight() + ")";
    }

}