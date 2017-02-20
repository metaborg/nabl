package org.metaborg.meta.nabl2.terms.generic;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Iterator;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IConsTerm;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.ListTerms;
import org.pcollections.PSet;

import com.google.common.collect.ImmutableClassToInstanceMap;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class ConsTerm implements IConsTerm {

    @Value.Parameter @Override public abstract ITerm getHead();

    @Value.Parameter @Override public abstract IListTerm getTail();

    @Value.Lazy @Override public boolean isGround() {
        return getHead().isGround() && getTail().isGround();
    }

    @Value.Lazy @Override public PSet<ITermVar> getVars() {
        return getHead().getVars().plusAll(getTail().getVars());
    }

    @Value.Default @Value.Auxiliary @Override public ImmutableClassToInstanceMap<Object> getAttachments() {
        return ImmutableClassToInstanceMap.<Object>builder().build();
    }

    @Override public <T> T match(ITerm.Cases<T> cases) {
        return cases.caseList(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(ITerm.CheckedCases<T, E> cases) throws E {
        return cases.caseList(this);
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseCons(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseCons(this);
    }

    @Override public Iterator<ITerm> iterator() {
        return new ListTermIterator(this);
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