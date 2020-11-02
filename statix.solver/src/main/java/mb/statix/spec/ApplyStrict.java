package mb.statix.spec;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.List;
import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.u.PersistentUnifier;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;

class ApplyStrict extends ApplyMode<Delay> {

    @Override Optional<ApplyResult> apply(IState.Immutable state, Rule rule, List<? extends ITerm> args,
            IConstraint cause) throws Delay {
        final ISubstitution.Transient subst;
        final Optional<ISubstitution.Immutable> matchResult =
                P.match(rule.params(), args, state.unifier()).orElseThrow(vars -> Delay.ofVars(vars));
        if((subst = matchResult.map(u -> u.melt()).orElse(null)) == null) {
            return Optional.empty();
        }
        final ISubstitution.Immutable isubst = subst.freeze();
        final IConstraint newBody = rule.body().apply(isubst).withCause(cause);
        final ICompleteness.Immutable newBodyCriticalEdges =
                rule.bodyCriticalEdges() == null ? null : rule.bodyCriticalEdges().apply(isubst);
        final ApplyResult applyResult = ApplyResult.of(state, PersistentUnifier.Immutable.of(), Optional.empty(),
                newBody, newBodyCriticalEdges);
        return Optional.of(applyResult);
    }

}