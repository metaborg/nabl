package mb.nabl2.terms.build;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;

@Value.Immutable(lazyhash = false)
@Serial.Version(value = 42L)
abstract class AAppl0Term extends AbstractApplTerm implements IApplTerm {

    private static final List<ITerm> EMPTY_ARGS = ImmutableList.of();

    @Override @Value.Check protected AAppl0Term check() {
        return this;
    }

    @Value.Parameter @Override public abstract String getOp();

    @Override public List<ITerm> getArgs() {
        return EMPTY_ARGS;
    }

    @Override public int getArity() {
        return 0;
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
