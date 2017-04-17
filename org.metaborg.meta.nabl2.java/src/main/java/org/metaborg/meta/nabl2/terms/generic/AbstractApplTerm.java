package org.metaborg.meta.nabl2.terms.generic;

import java.util.List;
import java.util.Objects;

import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

public abstract class AbstractApplTerm extends AbstractTerm implements IApplTerm {

    @Value.Parameter @Override public abstract String getOp();

    @Value.Parameter @Override public abstract List<ITerm> getArgs();

    @Override public int getArity() {
        return getArgs().size();
    }

    @Value.Lazy @Override public boolean isGround() {
        return getArgs().stream().allMatch(ITerm::isGround);
    }

    @Value.Lazy @Override public PSet<ITermVar> getVars() {
        PSet<ITermVar> vars = HashTreePSet.empty();
        for(ITerm arg : getArgs()) {
            vars = vars.plusAll(arg.getVars());
        }
        return vars;
    }

    @Override public <T> T match(Cases<T> cases) {
        return cases.caseAppl(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
        return cases.caseAppl(this);
    }

    @Override public int hashCode() {
        return Objects.hash(getOp(), getArgs());
    }

    @Override public boolean equals(Object other) {
        if(other == null) {
            return false;
        }
        if(!(other instanceof IApplTerm)) {
            return false;
        }
        IApplTerm that = (IApplTerm) other;
        if(!getOp().equals(that.getOp())) {
            return false;
        }
        if(getArity() != getArity()) {
            return false;
        }
        for(int i = 0; i < getArity(); i++) {
            if(!getArgs().get(i).equals(that.getArgs().get(i))) {
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