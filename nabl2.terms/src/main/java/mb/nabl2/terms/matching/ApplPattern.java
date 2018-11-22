package mb.nabl2.terms.matching;

import static mb.nabl2.terms.matching.CheckedTermMatch.CM;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class ApplPattern extends Pattern {

    private final String op;
    private final List<Pattern> args;

    public ApplPattern(String op, Iterable<? extends Pattern> args) {
        this.op = op;
        this.args = ImmutableList.copyOf(args);
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
        // @formatter:off
        return CM.<Boolean, InsufficientInstantiationException>cases(
            CM.appl(applTerm -> {
                if(applTerm.getOp().equals(op) && applTerm.getArity() == args.size()
                        && matchTerms(args, applTerm.getArgs(), subst, unifier)) {
                    return true;
                } else {
                    return false;
                }
            }),
            CM.var(v -> {
                throw new InsufficientInstantiationException(v);
            })
        ).matchOrThrow(term, unifier).orElse(false);
        // @formatter:on
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(op).append("(").append(args).append(")");
        return sb.toString();
    }

}