package org.metaborg.meta.nabl2.regexp;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable
@Serial.Structural
abstract class Symbol<S> implements IRegExp<S> {

    public abstract S getSymbol();

    public abstract IRegExpBuilder<S> getBuilder();

    @Override public boolean isNullable() {
        return false;
    }

    @Override public <T> T accept(IRegExpFunction<S,T> visitor) {
        return visitor.symbol(getSymbol());
    }

    @Override public String toString() {
        return getSymbol().toString();
    }

}
