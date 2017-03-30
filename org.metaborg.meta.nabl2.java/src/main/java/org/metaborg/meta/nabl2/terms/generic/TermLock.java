package org.metaborg.meta.nabl2.terms.generic;

import java.io.Serializable;
import java.util.Iterator;

import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.pcollections.PSet;

import com.google.common.collect.ImmutableClassToInstanceMap;

final class TermLock implements ITerm, IListTerm, Serializable {

    private static final long serialVersionUID = 42L;

    private final ITerm term;

    TermLock(ITerm term) {
        this.term = term;
    }

    @Override public boolean isGround() {
        return term.isGround();
    }

    @Override public PSet<ITermVar> getVars() {
        return term.getVars();
    }

    @Override public ImmutableClassToInstanceMap<Object> getAttachments() {
        return term.getAttachments();
    }

    @Override public TermLock withAttachments(ImmutableClassToInstanceMap<Object> value) {
        return new TermLock(term.withAttachments(value));
    }

    @Override public <T> T match(ITerm.Cases<T> cases) {
        return cases.caseLock(term);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(ITerm.CheckedCases<T, E> cases) throws E {
        return cases.caseLock(term);
    }

    @Override public <T> T match(IListTerm.Cases<T> cases) {
        return cases.caseLock((IListTerm) term);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IListTerm.CheckedCases<T, E> cases) throws E {
        return cases.caseLock((IListTerm) term);
    }

    @Override public Iterator<ITerm> iterator() {
        return ((IListTerm) term).iterator();
    }

    @Override public int hashCode() {
        return term.hashCode();
    }

    @Override public boolean equals(Object obj) {
        return term.equals(obj);
    }

    @Override public String toString() {
        return "." + term;
    }

    public static TermLock of(ITerm term) {
        return new TermLock(term);
    }

}