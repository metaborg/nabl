package mb.nabl2.terms.matching;

import java.util.List;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.Tuple2;

@Value.Immutable
@Serial.Version(42L)
public abstract class MatchResult {

    @Value.Parameter public abstract ISubstitution.Immutable substitution();

    @Value.Parameter public abstract Set<ITermVar> constrainedVars();

    @Value.Parameter public abstract List<Tuple2<ITerm, ITerm>> equalities();

}