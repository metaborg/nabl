package org.metaborg.meta.nabl2.terms.generic;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class ApplTerm extends AbstractApplTerm implements IApplTerm {

    @Value.Parameter @Override public abstract String getOp();

    @Value.Parameter @Override public abstract List<ITerm> getArgs();

    @Value.Check @Override protected ApplTerm check() {
        if(isLocked() && getArgs().stream().anyMatch(arg -> !arg.isLocked())) {
            return ImmutableApplTerm.copyOf(this)
                    .withArgs(getArgs().stream().map(arg -> arg.withLocked(true)).collect(Collectors.toList()));
        }
        return this;
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseAppl(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseAppl(this);
    }

    @Override public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override public int hashCode() {
        return super.hashCode();
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getOp());
        sb.append("(");
        boolean first = true;
        for(ITerm arg : getArgs()) {
            if(first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(arg.toString());
        }
        sb.append(")");
        return sb.toString();
    }

}