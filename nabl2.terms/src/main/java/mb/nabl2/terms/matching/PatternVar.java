package mb.nabl2.terms.matching;

import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.build.TermBuild;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class PatternVar extends Pattern {

    private final @Nullable ITermVar var;

    public PatternVar() {
        this.var = null;
    }

    public PatternVar(String name) {
        this(TermBuild.B.newVar("", name));
    }

    public PatternVar(ITermVar var) {
        if(var == null) {
            throw new IllegalArgumentException();
        }
        this.var = var;
    }

    public boolean isWildcard() {
        return var == null;
    }

    @Override public Set<ITermVar> getVars() {
        return ImmutableSet.of(var);
    }

    @Override protected void matchTerm(ITerm term, Transient subst, IUnifier unifier)
            throws MismatchException, InsufficientInstantiationException {
        if(isWildcard()) {
            return;
        }
        if(subst.contains(var)) {
            final ITerm boundTerm = subst.apply(var);
            if(unifier.areEqual(boundTerm, term)) {
                return;
            } else if(unifier.areUnequal(boundTerm, term)) {
                throw new MismatchException(this, term);
            } else {
                throw new InsufficientInstantiationException(this);
            }
        } else {
            subst.put(var, term);
        }
    }

    @Override public String toString() {
        return isWildcard() ? "_" : var.toString();
    }

}