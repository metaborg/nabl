package org.metaborg.meta.nabl2.terms.generic;

import java.util.Iterator;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IAnnotation;
import org.metaborg.meta.nabl2.terms.IConsTerm;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;

import com.google.common.collect.ImmutableClassToInstanceMap;

@Value.Immutable
@Serial.Structural
abstract class ConsTerm implements IConsTerm {

    @Value.Parameter @Override public abstract ITerm getHead();

    @Value.Parameter @Override public abstract IListTerm getTail();

    @Value.Lazy @Override public boolean isGround() {
        return getHead().isGround() && getTail().isGround();
    }

    @Value.Lazy @Override public int getLength() {
        return 1 + getTail().getLength();
    }

    @Value.Default @Value.Auxiliary @Override public ImmutableClassToInstanceMap<IAnnotation> getAnnotations() {
        return ImmutableClassToInstanceMap.<IAnnotation> builder().build();
    }

    @Override public <T> T match(ITerm.Cases<T> cases) {
        return cases.caseList(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(ITerm.CheckedCases<T,E> cases) throws E {
        return cases.caseList(this);
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseCons(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E {
        return cases.caseCons(this);
    }

    @Override public Iterator<ITerm> iterator() {
        return new ListTermIterator(this);
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        boolean first = true;
        for (ITerm elem : this) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(elem.toString());
        }
        sb.append(")");
        return sb.toString();
    }

}