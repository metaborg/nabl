package mb.nabl2.terms.matching;

import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action2;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.build.Attachments;
import mb.nabl2.terms.build.TermBuild;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.u.IUnifier;

public final class PatternAs extends Pattern {
    private static final long serialVersionUID = 1L;

    private final PatternVar var;
    private final Pattern pattern;

    PatternAs(String name, Pattern pattern) {
        this(TermBuild.B.newVar("", name), pattern);
    }

    PatternAs(ITermVar var, Pattern pattern) {
        super(Attachments.empty());
        this.var = new PatternVar(var);
        this.pattern = pattern;
    }

    PatternAs(PatternVar var, Pattern pattern) {
        super(Attachments.empty());
        this.var = var;
        this.pattern = pattern;
    }

    PatternAs(Pattern pattern) {
        super(Attachments.empty());
        this.var = new PatternVar();
        this.pattern = pattern;
    }

    public @Nullable ITermVar getVar() {
        return var.getVar();
    }

    public Pattern getPattern() {
        return pattern;
    }

    @Override public Set<ITermVar> getVars() {
        Set.Transient<ITermVar> vars = CapsuleUtil.transientSet();
        vars.__insertAll(var.getVars());
        vars.__insertAll(pattern.getVars());
        return vars.freeze();
    }

    @Override public boolean isConstructed() {
        return pattern.isConstructed();
    }

    @Override protected boolean matchTerm(ITerm term, ISubstitution.Transient subst, IUnifier.Immutable unifier,
            Eqs eqs) {
        return matchTerms(Iterables2.from(var, pattern), Iterables2.from(term, term), subst, unifier, eqs);
    }

    @Override public PatternAs apply(IRenaming subst) {
        return new PatternAs(var.apply(subst), pattern.apply(subst));
    }

    @Override public PatternAs eliminateWld(Function0<ITermVar> fresh) {
        return new PatternAs(var.eliminateWld(fresh), pattern.eliminateWld(fresh));
    }

    @Override protected ITerm asTerm(Action2<ITermVar, ITerm> equalities,
            Function1<Optional<ITermVar>, ITermVar> fresh) {
        final ITerm term = pattern.asTerm(equalities, fresh);
        if(var.isWildcard()) {
            return term;
        }
        equalities.apply(var.getVar(), term);
        return var.getVar();
    }

    @Override public String toString() {
        return var.toString() + "@" + pattern.toString();
    }

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        PatternAs patternAs = (PatternAs) o;
        return Objects.equals(var, patternAs.var) && Objects.equals(pattern, patternAs.pattern);
    }

    @Override public int hashCode() {
        return Objects.hash(var, pattern);
    }
}
