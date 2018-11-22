package mb.nabl2.terms.matching;

import java.util.Set;

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
        this(TermBuild.B.newVar("'", name), pattern);
    }

    public PatternAs(ITermVar var, Pattern pattern) {
        this.var = new PatternVar(var);
        this.pattern = pattern;
    }

    public PatternVar getVar() {
        return var;
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

    @Override protected boolean matchTerm(ITerm term, Transient subst, IUnifier unifier)
            throws InsufficientInstantiationException {
        return var.matchTerm(term, subst, unifier) && pattern.matchTerm(term, subst, unifier);
    }

    @Override public String toString() {
        return var.toString() + "@" + pattern.toString();
    }

}