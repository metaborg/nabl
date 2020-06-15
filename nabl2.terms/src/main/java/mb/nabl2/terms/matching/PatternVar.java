package mb.nabl2.terms.matching;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Action2;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.build.TermBuild;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.u.IUnifier;

class PatternVar extends Pattern {
    private static final long serialVersionUID = 1L;

    private final @Nullable ITermVar var;

    public PatternVar() {
        super(ImmutableClassToInstanceMap.of());
        this.var = null;
    }

    public PatternVar(String name) {
        this(TermBuild.B.newVar("", name));
    }

    public PatternVar(ITermVar var) {
        super(var != null ? var.getAttachments() : ImmutableClassToInstanceMap.of());
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
            final Optional<? extends IUnifier.Immutable> diff = unifier.diff(subst.apply(var), term);
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

    @Override public PatternVar apply(IRenaming subst) {
        return isWildcard() ? this : new PatternVar(subst.rename(var));
    }

    @Override public PatternVar eliminateWld(Function0<ITermVar> fresh) {
        return isWildcard() ? new PatternVar(fresh.apply()) : this;
    }

    @Override protected ITerm asTerm(Action2<ITermVar, ITerm> equalities,
            Function1<Optional<ITermVar>, ITermVar> fresh) {
        return fresh.apply(Optional.ofNullable(var));
    }

    @Override public String toString() {
        return isWildcard() ? "_" : var.toString();
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        PatternVar that = (PatternVar)o;
        return Objects.equals(var, that.var);
    }

    @Override
    public int hashCode() {
        return Objects.hash(var);
    }
}
