package mb.nabl2.terms.matching;

import java.util.Set;

import javax.annotation.Nullable;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.build.TermBuild;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class PatternAs extends Pattern {

    private final PatternVar var;
    private final Pattern pattern;

    public PatternAs(String name, Pattern pattern) {
        this(TermBuild.B.newVar("", name), pattern);
    }

    public PatternAs(ITermVar var, Pattern pattern) {
        this.var = new PatternVar(var);
        this.pattern = pattern;
    }

    PatternAs(PatternVar var, Pattern pattern) {
        this.var = var;
        this.pattern = pattern;
    }

    PatternAs(Pattern pattern) {
        this.var = new PatternVar();
        this.pattern = pattern;
    }

    @Nullable ITermVar getVar() {
        return var.getVar();
    }

    public Pattern getPattern() {
        return pattern;
    }

    @Override public Set<ITermVar> getVars() {
        ImmutableSet.Builder<ITermVar> vars = ImmutableSet.builder();
        vars.addAll(var.getVars());
        vars.addAll(pattern.getVars());
        return vars.build();
    }

    @Override protected MaybeNotInstantiated<Boolean> matchTerm(ITerm term, Transient subst, IUnifier unifier) {
        return matchTerms(Iterables2.from(var, pattern), Iterables2.from(term, term), subst, unifier);
    }

    @Override public String toString() {
        return var.toString() + "@" + pattern.toString();
    }

}