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

    @Nullable ITermVar getVar() {
        return var;
    }

    public boolean isWildcard() {
        return var == null;
    }

    @Override public Set<ITermVar> getVars() {
        return ImmutableSet.of(var);
    }

    @Override protected MaybeNotInstantiated<Boolean> matchTerm(ITerm term, Transient subst, IUnifier unifier) {
        if(isWildcard()) {
            return MaybeNotInstantiated.ofResult(true);
        } else if(subst.contains(var)) {
            return unifier.areEqual(subst.apply(var), term);
        } else {
            subst.put(var, term);
            return MaybeNotInstantiated.ofResult(true);
        }
    }

    @Override public String toString() {
        return isWildcard() ? "_" : var.toString();
    }

}