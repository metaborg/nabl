package mb.nabl2.terms.build;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.Terms;

@Value.Immutable(lazyhash = false)
@Serial.Version(value = 42L)
abstract class AAppl1Term extends AbstractApplTerm implements IApplTerm {

    @Override @Value.Check protected AAppl1Term check() {
        if(Terms.TUPLE_OP.equals(getOp())) {
            throw new IllegalArgumentException("1-tuples are not supported.");
        }
        return this;
    }

    @Value.Parameter @Override public abstract String getOp();

    @Value.Parameter public abstract ITerm getArg0();

    @Override public List<ITerm> getArgs() {
        return ImmutableList.of(getArg0());
    }

    @Override public int getArity() {
        return 1;
    }

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
