package mb.statix.spec;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class AType {

    @Value.Parameter public abstract List<ITerm> getInputs();

    @Value.Parameter public abstract List<ITerm> getOutputs();

    public int getArity() {
        return getInputArity() + getOutputArity();
    }

    public int getInputArity() {
        return getInputs().size();
    }

    public int getOutputArity() {
        return getOutputs().size();
    }

}