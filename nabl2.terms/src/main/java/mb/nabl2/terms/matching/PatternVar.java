package mb.nabl2.terms.matching;

import java.util.Optional;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.metaborg.util.functions.Action2;
import org.metaborg.util.functions.Function0;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.build.TermBuild;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.IUnifier.Immutable;

class PatternVar extends Pattern {
    private static final long serialVersionUID = 1L;

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
        return isWildcard() ? ImmutableSet.of() : ImmutableSet.of(var);
    }

    @Override protected boolean matchTerm(ITerm term, Transient subst, IUnifier.Immutable unifier, Eqs eqs) {
        if(isWildcard()) {
            return true;
        } else if(subst.contains(var)) {
            final Optional<Immutable> diff = unifier.diff(subst.apply(var), term);
            if(!diff.isPresent()) {
                return false;
            }
            diff.get().equalityMap().forEach(eqs::add);
            return true;
        } else {
            subst.put(var, term);
            return true;
        }
    }

    @Override protected ITerm asTerm(Action2<ITermVar, ITerm> equalities, Function0<ITermVar> fresh) {
        if(isWildcard()) {
            return fresh.apply();
        }
        return var;
    }

    @Override public String toString() {
        return isWildcard() ? "_" : var.toString();
    }

}