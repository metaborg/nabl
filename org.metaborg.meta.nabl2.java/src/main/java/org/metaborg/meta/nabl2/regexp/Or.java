package org.metaborg.meta.nabl2.regexp;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable
@Serial.Structural
abstract class Or<S> implements IRegExp<S> {

    @Value.Parameter public abstract IRegExp<S> getLeft();

    @Value.Parameter public abstract IRegExp<S> getRight();

    @Value.Parameter public abstract IRegExpBuilder<S> getBuilder();

    @Value.Lazy @Override public boolean isNullable() {
        return getLeft().isNullable() || getRight().isNullable();
    }

    @Override public <T> T match(IRegExpCases<S,T> visitor) {
        return visitor.or(getLeft(), getRight());
    }

    @Override public String toString() {
        return "(" + getLeft() + " | " + getRight() + ")";
    }

}
