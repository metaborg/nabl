package org.metaborg.meta.nabl2.regexp;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable
@Serial.Structural
abstract class Complement<S> implements IRegExp<S> {

    public abstract IRegExp<S> getRE();

    public abstract IRegExpBuilder<S> getBuilder();

    @Value.Lazy @Override public boolean isNullable() {
        return !getRE().isNullable();
    }

    @Override public <T> T accept(IRegExpFunction<S,T> visitor) {
        return visitor.complement(getRE());
    }

    @Override public String toString() {
        return "~(" + getRE() + ")";
    }

}
