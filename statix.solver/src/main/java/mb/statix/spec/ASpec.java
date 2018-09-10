package mb.statix.spec;

import java.util.Map;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import mb.nabl2.regexp.IAlphabet;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ASpec {

    @Value.Parameter public abstract ListMultimap<String, Rule> rules();

    @Value.Parameter public abstract IAlphabet<ITerm> labels();

    @Value.Parameter public abstract ITerm endOfPath();

    @Value.Parameter public abstract Map<ITerm, Type> relations();

    @Value.Parameter public abstract Multimap<String, Tuple2<Integer, ITerm>> scopeExtensions();

}