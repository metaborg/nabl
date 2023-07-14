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

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder("StrategoAnnotations{");
        b.append("annotationList=").append(getAnnotationList());
        return b.append('}').toString();
    }
}
