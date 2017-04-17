package org.metaborg.meta.nabl2.util.tuples;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class Tuple3<T1, T2, T3> {

    @Value.Parameter public abstract T1 _1();

    @Value.Parameter public abstract T2 _2();

    @Value.Parameter public abstract T3 _3();

}