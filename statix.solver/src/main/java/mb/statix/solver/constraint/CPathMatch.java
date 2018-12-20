package mb.statix.solver.constraint;

import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.ConstraintResult;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;
import mb.statix.spoofax.StatixTerms;

/**
 * Implementation for a path match constraint.
 * 
 * <pre>pathMatch[pathRegex](labelsTerm)</pre>
 */
public class CPathMatch implements IConstraint {

    private final IRegExpMatcher<ITerm> re;
    private final IListTerm labelsTerm;

    private final @Nullable IConstraint cause;

    /**
     * Creates a new path match constraint with the given path regex and the given labels.
     * No cause for this constraint is added.
     * 
     * @param re
     *      the path regex
     * @param labelsTerm
     *      the term representing the list of labels
     */
    public CPathMatch(IRegExp<ITerm> re, IListTerm labelsTerm) {
        this(RegExpMatcher.create(re), labelsTerm, null);
    }

    /**
     * Creates a new path match constraint with the given path regex and the given labels, with a
     * causing constraint.
     * 
     * @param re
     *      the path regex
     * @param labelsTerm
     *      the term representing the list of labels
     * @param cause
     *      the constraint that caused this constraint
     */
    private CPathMatch(IRegExpMatcher<ITerm> re, IListTerm labelsTerm, @Nullable IConstraint cause) {
        this.re = re;
        this.labelsTerm = labelsTerm;
        this.cause = cause;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CPathMatch withCause(@Nullable IConstraint cause) {
        return new CPathMatch(re, labelsTerm, cause);
    }

    @Override public CPathMatch apply(ISubstitution.Immutable subst) {
        return new CPathMatch(re, (IListTerm) subst.apply(labelsTerm), cause);
    }

    /**
     * @see IConstraint#solve
     * 
     * @throws IllegalArgumentException
     *      If the given labels term contains non-labels.
     * @throws Delay
     *      If one of the given label terms is not ground.
     * @throws Delay
     *      If the given labels term is not ground.
     */
    @Override public Optional<ConstraintResult> solve(State state, ConstraintContext params) throws Delay {
        final IUnifier unifier = state.unifier();
        // @formatter:off
        return ((IListTerm) unifier.findTerm(labelsTerm)).matchOrThrow(ListTerms.checkedCases(
            cons -> {
                final ITerm labelTerm = cons.getHead();
                if(!unifier.isGround(labelTerm)) {
                    throw Delay.ofVars(unifier.getVars(labelTerm));
                }
                final ITerm label = StatixTerms.label().match(labelTerm, unifier)
                        .orElseThrow(() -> new IllegalArgumentException("Expected label, got " + unifier.toString(labelTerm)));
                final IRegExpMatcher<ITerm> re = this.re.match(label);
                if(re.isEmpty()) {
                    return Optional.empty();
                } else {
                    return Optional.of(ConstraintResult.of(state, ImmutableSet.of(new CPathMatch(re, cons.getTail(), cause))));
                }
            },
            nil -> {
                if(re.isAccepting()) {
                    return Optional.of(ConstraintResult.of(state, ImmutableSet.of()));
                } else {
                    return Optional.empty();
                }
            },
            var -> {
                throw Delay.ofVar(var);
            }
        ));
        // @formatter:on
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("pathMatch[");
        sb.append(re);
        sb.append("](");
        sb.append(termToString.format(labelsTerm));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}