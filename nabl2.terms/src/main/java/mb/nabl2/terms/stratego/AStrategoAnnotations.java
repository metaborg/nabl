package mb.nabl2.terms.stratego;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.spoofax.interpreter.terms.IStrategoTerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class AStrategoAnnotations {

    @Value.Parameter public abstract List<IStrategoTerm> getAnnotationList();

}
