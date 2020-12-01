package mb.nabl2.util;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.functions.Function4;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ATuple4<T1, T2, T3, T4> {

    @Value.Parameter public abstract T1 _1();

    @Value.Parameter public abstract T2 _2();

    @Value.Parameter public abstract T3 _3();

    @Value.Parameter public abstract T4 _4();

    public <R> R apply(Function4<T1, T2, T3, T4, R> f) {
        return f.apply(_1(), _2(), _3(), _4());
    }

}