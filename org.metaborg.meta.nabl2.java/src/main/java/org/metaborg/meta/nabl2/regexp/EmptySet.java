package org.metaborg.meta.nabl2.regexp;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable
@Serial.Structural
abstract class EmptySet<S> implements IRegExp<S> {

    @Value.Parameter public abstract IRegExpBuilder<S> getBuilder();

    @Override public boolean isNullable() {
        return false;
    }

    @Override public <T> T match(IRegExpCases<S,T> visitor) {
        return visitor.emptySet();
    }

    @Override public String toString() {
        return "0";
    }

}