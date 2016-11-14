package org.metaborg.meta.nabl2.regexp;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable
@Serial.Structural
public abstract class And<S> implements IRegExp<S> {

    public abstract IRegExp<S> getLeft();

    public abstract IRegExp<S> getRight();

    public abstract IRegExpBuilder<S> getBuilder();

    @Value.Lazy @Override public boolean isNullable() {
        return getLeft().isNullable() && getRight().isNullable();
    }

    @Override public <T> T accept(IRegExpFunction<S,T> visitor) {
        return visitor.and(getLeft(), getRight());
    }

    @Override public String toString() {
        return "(" + getLeft() + " & " + getRight() + ")";
    }

}