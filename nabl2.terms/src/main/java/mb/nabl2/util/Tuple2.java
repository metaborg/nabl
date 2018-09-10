package mb.nabl2.util;

import java.util.Map;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.functions.Function2;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class Tuple2<T1, T2> implements Map.Entry<T1, T2> {

    @Value.Parameter public abstract T1 _1();

    @Value.Parameter public abstract T2 _2();

    @Override public T1 getKey() {
        return _1();
    }

    @Override public T2 getValue() {
        return _2();
    }

    @Override public T2 setValue(T2 value) {
        throw new UnsupportedOperationException("Method Tuple2::setValue not supported.");
    }

    public <R> R apply(Function2<T1, T2, R> f) {
        return f.apply(_1(), _2());
    }

    public static <T1, T2> Tuple2<T1, T2> of(Map.Entry<T1, T2> entry) {
        return ImmutableTuple2.of(entry.getKey(), entry.getValue());
    }

}
