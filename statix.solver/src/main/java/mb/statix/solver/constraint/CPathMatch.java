package mb.statix.solver.constraint;

import java.util.Optional;

import javax.annotation.Nullable;

import org.metaborg.util.Ref;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.ConstraintResult;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;
import mb.statix.spoofax.StatixTerms;

public class CPathMatch implements IConstraint {

    private final IRegExp<ITerm> re;
    private final IListTerm labelsTerm;

    private final @Nullable IConstraint cause;

    public CPathMatch(IRegExp<ITerm> re, IListTerm labelsTerm) {
        this(re, labelsTerm, null);
    }

    public CPathMatch(IRegExp<ITerm> re, IListTerm labelsTerm, @Nullable IConstraint cause) {
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

    @Override public Optional<ConstraintResult> solve(State state, ConstraintContext params) throws Delay {
        final IUnifier unifier = state.unifier();
        IListTerm labels = labelsTerm;
        Ref<IRegExpMatcher<ITerm>> re = new Ref<>(RegExpMatcher.create(this.re));
        Ref<ITermVar> varTail = new Ref<>();
        while(labels != null) {
            // @formatter:off
            labels = labels.match(ListTerms.cases(
                cons -> {
                    final ITerm labelTerm = cons.getHead();
                    if(!unifier.isGround(labelTerm)) {
                        return null;
                    }
                    final ITerm label = StatixTerms.label().match(labelTerm, unifier)
                            .orElseThrow(() -> new IllegalArgumentException("Expected label, got " + unifier.toString(labelTerm)));
                    re.set(re.get().match(label));
                    if(re.get().isEmpty()) {
                        return null;
                    }
                    return cons.getTail();
                },
                nil -> {
                    return null;
                },
                var -> {
                    varTail.set(var);
                    return null;
                }
            ));
            // @formatter:on
        }
        if(varTail.get() == null) { // we got a complete list
            if(re.get().isAccepting()) {
                return Optional.of(ConstraintResult.of(state, ImmutableSet.of()));
            } else {
                return Optional.empty();
            }
        } else { // we got a partial list
            if(re.get().isEmpty()) {
                return Optional.empty();
            } else {
                throw Delay.ofVar(varTail.get());
            }
        }
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("pathMatch[");
        sb.append(re);
        sb.append("](");
        sb.append(unifier.toString(labelsTerm));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}