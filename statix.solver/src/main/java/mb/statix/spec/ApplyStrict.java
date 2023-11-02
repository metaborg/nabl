package mb.statix.spec;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.List;
import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;

class ApplyStrict extends ApplyMode<Delay> {

    @Override Optional<ApplyResult> apply(
            IUniDisunifier.Immutable unifier,
            Rule rule,
            List<? extends ITerm> args,
            IConstraint cause,
            Safety safety,
            boolean trackOrigins
    ) throws Delay {
        final ISubstitution.Immutable subst = P.match(rule.params(), args, unifier)
                .orElseThrow(vars -> Delay.ofVars(vars)).orElse(null);
        if (subst == null) {
            return Optional.empty();
        }
        final IConstraint newBody;
        if (safety.equals(Safety.UNSAFE)) {
            newBody = rule.body().unsafeApply(subst, trackOrigins).withCause(cause);
        } else {
            newBody = rule.body().apply(subst, trackOrigins).withCause(cause);
        }
        final ICompleteness.Immutable newBodyCriticalEdges =
                rule.bodyCriticalEdges() == null ? null : rule.bodyCriticalEdges().apply(subst);
        final ApplyResult applyResult = ApplyResult.of(
                Optional.empty(),
                newBody,
                newBodyCriticalEdges != null ? newBodyCriticalEdges : Completeness.Immutable.of(),
                subst
        );
        return Optional.of(applyResult);
    }

}