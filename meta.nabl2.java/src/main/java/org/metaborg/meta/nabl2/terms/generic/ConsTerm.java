package org.metaborg.meta.nabl2.terms.generic;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Objects;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IConsTerm;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.ListTerms;

import com.google.common.collect.ImmutableMultiset;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class ConsTerm extends AbstractTerm implements IConsTerm {

    @Value.Parameter @Override public abstract ITerm getHead();

    @Value.Parameter @Override public abstract IListTerm getTail();

    @Value.Check public ConsTerm check() {
        if(isLocked() && !getHead().isLocked()) {
            return ImmutableConsTerm.copyOf(this).withHead(getHead().withLocked(true));
        }
        if(isLocked() && !getTail().isLocked()) {
            return ImmutableConsTerm.copyOf(this).withTail(getTail().withLocked(true));
        }
        return this;
    }

    @Value.Lazy @Override public boolean isGround() {
        return getHead().isGround() && getTail().isGround();
    }

    @Value.Default @Value.Auxiliary @Override public boolean isLocked() {
        return false;
    }

    @Value.Lazy @Override public ImmutableMultiset<ITermVar> getVars() {
        final ImmutableMultiset.Builder<ITermVar> vars = ImmutableMultiset.builder();
        vars.addAll(getHead().getVars());
        vars.addAll(getTail().getVars());
        return vars.build();
    }

    @Override public <T> T match(ITerm.Cases<T> cases) {
        return cases.caseList(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(ITerm.CheckedCases<T, E> cases) throws E {
        return cases.caseList(this);
    }

    @Override public <T> T match(IListTerm.Cases<T> cases) {
        return cases.caseCons(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IListTerm.CheckedCases<T, E> cases) throws E {
        return cases.caseCons(this);
    }

    @Override public int hashCode() {
        return Objects.hash(getHead(), getTail());
    }

    @Override public boolean equals(Object other) {
        if(other == null) {
            return false;
        }
        if(!(other instanceof IConsTerm)) {
            return false;
        }
        IConsTerm that = (IConsTerm) other;
        if(!getHead().equals(that.getHead())) {
            return false;
        }
        if(!getTail().equals(that.getTail())) {
            return false;
        }
        return true;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(getHead());
        match(ListTerms.casesFix(
            // @formatter:off
            (f,cons) -> {
                sb.append(",");
                sb.append(cons.getHead());
                return cons.getTail().match(f);
            },
            (f,nil) -> unit,
            (f,var) -> {
                sb.append("|");
                sb.append(var);
                return unit;
            }
            // @formatter:on
        ));
        sb.append("]");
        return sb.toString();
    }

}