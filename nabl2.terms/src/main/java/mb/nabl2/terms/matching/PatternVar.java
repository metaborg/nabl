package mb.nabl2.terms.matching;

import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action2;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.build.Attachments;
import mb.nabl2.terms.build.TermBuild;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.u.IUnifier;

public final class PatternVar extends Pattern {
    private static final long serialVersionUID = 1L;

    private final @Nullable ITermVar var;

    PatternVar() {
        super(Attachments.empty());
        this.var = null;
    }

    PatternVar(String name) {
        this(TermBuild.B.newVar("", name));
    }

    PatternVar(ITermVar var) {
        super(var != null ? var.getAttachments() : Attachments.empty());
        if(var == null) {
            throw new IllegalArgumentException();
        }
        this.var = var;
    }

    @Nullable ITermVar getVar() {
        return var;
    }

    @Override public boolean isConstructed() {
        return false;
    }

    public boolean isWildcard() {
        return var == null;
    }

    @Override public Set<ITermVar> getVars() {
        return isWildcard() ? CapsuleUtil.immutableSet() : CapsuleUtil.immutableSet(var);
    }

    @Override protected boolean matchTerm(ITerm term, ISubstitution.Transient subst, IUnifier.Immutable unifier,
            Eqs eqs) {
        if(isWildcard()) {
            return true;
        } else if(subst.contains(var)) {
            final IUnifier.Immutable diff;
            if((diff = unifier.diff(subst.apply(var), term).orElse(null)) == null) {
                return false;
            }
            for(ITermVar var : diff.domainSet()) {
                eqs.add(var, diff.findTerm(var));
            }
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

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        PatternVar that = (PatternVar) o;
        return Objects.equals(var, that.var);
    }

    @Override public int hashCode() {
        return Objects.hash(var);
    }
}
