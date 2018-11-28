package mb.nabl2.terms.matching;

import static mb.nabl2.terms.matching.CheckedTermMatch.CM;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.CheckedTermMatch.ICheckedMatcher;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class ApplPattern extends Pattern {

    private final String op;
    private final List<Pattern> args;
    private final ICheckedMatcher<IApplTerm, InsufficientInstantiationException> matcher;

    public ApplPattern(String op, Iterable<? extends Pattern> args) {
        this.op = op;
        this.args = ImmutableList.copyOf(args);
        // @formatter:off
        this.matcher = CM.term(Terms.<Optional<IApplTerm>, InsufficientInstantiationException>checkedCases()
                .appl(applTerm -> {
                    if(applTerm.getArity() == this.args.size() && applTerm.getOp().equals(op)) {
                        return Optional.of(applTerm);
                    } else {
                        return Optional.empty();
                    }
                }).var(v -> {
                    throw new InsufficientInstantiationException(v);
                }).otherwise(t -> {
                    return Optional.empty();
                }));
        // @formatter:on
    }

    public String getOp() {
        return op;
    }

    public List<Pattern> getArgs() {
        return args;
    }

    @Override public Set<ITermVar> getVars() {
        ImmutableSet.Builder<ITermVar> vars = ImmutableSet.builder();
        for(Pattern arg : args) {
            vars.addAll(arg.getVars());
        }
        return vars.build();
    }

    @Override protected boolean matchTerm(ITerm term, Transient subst, IUnifier unifier)
            throws InsufficientInstantiationException {
        final Optional<IApplTerm> applTerm = matcher.matchOrThrow(term, unifier);
        if(!applTerm.isPresent()) {
            return false;
        }
        return matchTerms(args, applTerm.get().getArgs(), subst, unifier);
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(op).append("(").append(args).append(")");
        return sb.toString();
    }

}