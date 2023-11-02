package mb.statix.spec;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.collection.Sets;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.MatchResult;
import mb.nabl2.terms.matching.VarProvider;
import mb.nabl2.terms.substitution.FreshVars;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.Diseq;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.VoidException;
import mb.statix.constraints.CExists;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.solver.StateUtil;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;

class ApplyRelaxed extends ApplyMode<VoidException> {

    @Override Optional<ApplyResult> apply(
            IUniDisunifier.Immutable unifier,
            Rule rule,
            List<? extends ITerm> args,
            IConstraint cause,
            Safety safety,
            boolean trackOrigins
    ) throws VoidException {
        Set.Immutable<ITermVar> freeVars = rule.freeVars();
        for (ITerm arg : args) {
            freeVars = freeVars.__insertAll(arg.getVars());
        }

        // match and create equality constraints
        final FreshVars fresh = new FreshVars(freeVars);
        final VarProvider freshProvider = VarProvider.of(fresh::fresh, () -> fresh.fresh("_"));
        final MatchResult matchResult;
        if ((matchResult = P.matchWithEqs(rule.params(), args, unifier, freshProvider).orElse(null)) == null) {
            return Optional.empty();
        }

        // newly generated variables
        final Set.Immutable<ITermVar> generatedVars = fresh.reset();

        // non-generated variables that are constrained by the match
        final Set.Immutable<ITermVar> constrainedVars = Set.Immutable.subtract(matchResult.constrainedVars(), generatedVars);

        final IConstraint appliedBody;
        if (safety.equals(Safety.UNSAFE)) {
            appliedBody = rule.body().unsafeApply(matchResult.substitution(), trackOrigins).withCause(cause);
        } else {
            appliedBody = rule.body().apply(matchResult.substitution(), trackOrigins).withCause(cause);
        }
        final ICompleteness.Immutable appliedCriticalEdges =
                rule.bodyCriticalEdges() == null ? null : rule.bodyCriticalEdges().apply(matchResult.substitution());

        // simplify guard constraints
        final IUniDisunifier.Result<IUnifier.Immutable> unifyResult;
        try {
            if ((unifyResult = unifier.unify(matchResult.equalities()).orElse(null)) == null) {
                return Optional.empty();
            }
        } catch (OccursException e) {
            return Optional.empty();
        }
        final IUnifier.Immutable diff = unifyResult.result();

        // construct guard
        final IConstraint newBody;
        final ICompleteness.Immutable newCriticalEdges;
        final Optional<Diseq> diseq;
        final IUnifier.Immutable guard = diff.retainAll(constrainedVars).unifier();
        if (guard.isEmpty()) {
            newBody = appliedBody;
            newCriticalEdges = appliedCriticalEdges;
            diseq = Optional.empty();
        } else {
            final ICompleteness.Immutable newBodyCriticalEdges =
                    appliedCriticalEdges == null ? null : appliedCriticalEdges.retainAll(generatedVars, unifier);
            newBody = new CExists(generatedVars, Constraints.conjoin(StateUtil.asEqualities(diff), appliedBody), cause,
                    newBodyCriticalEdges);
            newCriticalEdges =
                    appliedCriticalEdges == null ? null : appliedCriticalEdges.removeAll(generatedVars, unifier);
            diseq = Optional.of(Diseq.of(generatedVars, guard));
        }

        // construct result
        final ApplyResult applyResult = ApplyResult.of(
                diseq,
                newBody,
                newCriticalEdges != null ? newCriticalEdges : Completeness.Immutable.of(),
                matchResult.substitution()
        );

        return Optional.of(applyResult);
    }

}