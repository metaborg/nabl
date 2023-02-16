package mb.nabl2.terms.stratego;

import java.util.stream.Collectors;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.ImList;
import org.spoofax.interpreter.terms.IStrategoTerm;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class AStrategoAnnotations {

    @Value.Parameter public abstract ImList.Immutable<IStrategoTerm> getAnnotationList();

    public boolean isEmpty() {
        return getAnnotationList().isEmpty();
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StrategoAnnotations");
        sb.append("(").append(getAnnotationList().stream().map(Object::toString)
            .collect(Collectors.joining(",", "[", "]"))).append(")");
        return sb.toString();
    }
}
