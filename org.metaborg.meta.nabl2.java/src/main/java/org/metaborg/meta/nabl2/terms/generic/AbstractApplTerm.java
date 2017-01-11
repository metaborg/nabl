package org.metaborg.meta.nabl2.terms.generic;

import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.Iterables;

public abstract class AbstractApplTerm implements IApplTerm {

    @Value.Default @Value.Auxiliary @Override public ImmutableClassToInstanceMap<Object> getAttachments() {
        return ImmutableClassToInstanceMap.<Object> builder().build();
    }

    @Value.Lazy @Override public int getArity() {
        return Iterables.size(getArgs());
    }

    @Value.Lazy @Override public boolean isGround() {
        boolean ground = true;
        for (ITerm arg : getArgs()) {
            ground &= arg.isGround();
        }
        return ground;
    }

    @Value.Lazy @Override public PSet<ITermVar> getVars() {
        PSet<ITermVar> vars = HashTreePSet.empty();
        for (ITerm arg : getArgs()) {
            vars = vars.plusAll(arg.getVars());
        }
        return vars;
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseAppl(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E {
        return cases.caseAppl(this);
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof IApplTerm)) {
            return false;
        }
        IApplTerm that = (IApplTerm) other;
        if (!getOp().equals(that.getOp())) {
            return false;
        }
        if (getArity() != getArity()) {
            return false;
        }
        for (int i = 0; i < getArity(); i++) {
            if (!getArgs().get(i).termEquals(that.getArgs().get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getOp());
        sb.append("(");
        boolean first = true;
        for (ITerm arg : getArgs()) {
            if (first) {
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