package mb.nabl2.terms.build;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.Terms;

@Value.Immutable(lazyhash = false)
@Serial.Version(value = 42L)
abstract class AApplTerm extends AbstractApplTerm implements IApplTerm {

    @Override @Value.Check protected AApplTerm check() {
        if(getArity() == 1 && Terms.TUPLE_OP.equals(getOp())) {
            throw new IllegalArgumentException("1-tuples are not supported.");
        }
        return this;
    }

    @Value.Parameter @Override public abstract String getOp();

    @Value.Parameter @Override public abstract List<ITerm> getArgs();

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseAppl(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseAppl(this);
    }

    @Override public int hashCode() {
        return super.hashCode();
    }

    @Override public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override public String toString() {
        return super.toString();
    }

}
