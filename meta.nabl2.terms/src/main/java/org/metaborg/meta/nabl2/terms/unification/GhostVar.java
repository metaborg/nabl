package org.metaborg.meta.nabl2.terms.unification;

import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

public class GhostVar implements ITermVar {

    private final boolean locked;
    private final ImmutableClassToInstanceMap<Object> attachments;

    private GhostVar(boolean locked, ImmutableClassToInstanceMap<Object> attachments) {
        this.locked = locked;
        this.attachments = attachments;
    }

    public String getResource() {
        return "";
    }

    public String getName() {
        return Integer.toString(System.identityHashCode(this));
    }

    public boolean isGround() {
        return false;
    }

    public boolean isLocked() {
        return locked;
    }

    public ITermVar withLocked(boolean locked) {
        return new GhostVar(locked, attachments);
    }

    public Multiset<ITermVar> getVars() {
        return ImmutableMultiset.of(this);
    }

    public ImmutableClassToInstanceMap<Object> getAttachments() {
        return attachments;
    }

    public ITermVar withAttachments(ImmutableClassToInstanceMap<Object> value) {
        return new GhostVar(locked, value);
    }

    public <T> T match(ITerm.Cases<T> cases) {
        return cases.caseVar(this);
    }

    public <T, E extends Throwable> T matchOrThrow(ITerm.CheckedCases<T, E> cases) throws E {
        return cases.caseVar(this);
    }

    public <T> T match(IListTerm.Cases<T> cases) {
        return cases.caseVar(this);
    }

    public <T, E extends Throwable> T matchOrThrow(IListTerm.CheckedCases<T, E> cases) throws E {
        return cases.caseVar(this);
    }

    @Override public String toString() {
        return "<?" + System.identityHashCode(this) + ">";
    }

    public static ITermVar of() {
        return new GhostVar(false, ImmutableClassToInstanceMap.builder().build());
    }

}