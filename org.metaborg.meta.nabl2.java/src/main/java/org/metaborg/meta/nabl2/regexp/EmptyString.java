package org.metaborg.meta.nabl2.regexp;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class EmptyString<S> implements IRegExp<S> {

    @Value.Parameter public abstract IRegExpBuilder<S> getBuilder();

    @Override public boolean isNullable() {
        return true;
    }

    @Override public <T> T match(IRegExpCases<S,T> visitor) {
        return visitor.emptyString();
    }

    @Override public String toString() {
        return "e";
    }

}
