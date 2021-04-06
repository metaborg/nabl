package mb.statix.spec;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

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
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.ICompleteness;

class ApplyRelaxed extends ApplyMode<VoidException> {

    @Override Optional<ApplyResult> apply(IUniDisunifier.Immutable unifier, Rule rule, List<? extends ITerm> args,
            IConstraint cause, Safety safety) throws VoidException {
        Set.Immutable<ITermVar> freeVars = rule.freeVars();
        for(ITerm arg : args) {
            freeVars = freeVars.__insertAll(arg.getVars());
        }

        // match and create equality constraints
        final FreshVars fresh = new FreshVars(freeVars, rule.body().vars());
        final VarProvider freshProvider = VarProvider.of(v -> fresh.fresh(v), () -> fresh.fresh("_"));
        final MatchResult matchResult;
        if((matchResult = P.matchWithEqs(rule.params(), args, unifier, freshProvider).orElse(null)) == null) {
            return Optional.empty();
        }

        // newly generated variables
        final Set.Immutable<ITermVar> generatedVars = fresh.reset();

        // non-generated variables that are constrained by the match
        final SetView<ITermVar> constrainedVars = Sets.difference(matchResult.constrainedVars(), generatedVars);

        final CExists appliedBody;
        if(safety.equals(Safety.UNSAFE)) {
            appliedBody = rule.body().unsafeApply(matchResult.substitution()).withCause(cause);
        } else {
            appliedBody = rule.body().apply(matchResult.substitution()).withCause(cause);
        }
        final ICompleteness.Immutable appliedCriticalEdges =
                rule.bodyCriticalEdges() == null ? null : rule.bodyCriticalEdges().apply(matchResult.substitution());

        // simplify guard constraints
        final IUniDisunifier.Result<IUnifier.Immutable> unifyResult;
        try {
            if((unifyResult = unifier.unify(matchResult.equalities()).orElse(null)) == null) {
                return Optional.empty();
            }
        } catch(OccursException e) {
            return Optional.empty();
        }
        final IUnifier.Immutable diff = unifyResult.result();

        // construct guard
        final CExists newBody;
        final ICompleteness.Immutable newCriticalEdges;
        final Optional<Diseq> diseq;
        final IUnifier.Immutable guard = diff.retainAll(constrainedVars).unifier();
        if(guard.isEmpty()) {
            newBody = appliedBody;
            newCriticalEdges = appliedCriticalEdges;
            diseq = Optional.empty();
        } else {
            ICompleteness.Immutable newBodyCriticalEdges = appliedBody.bodyCriticalEdges().orElse(null);
            if(appliedCriticalEdges == null) {
                newCriticalEdges = null;
            } else {
                if(newBodyCriticalEdges == null) {
                    newBodyCriticalEdges = appliedCriticalEdges.retainAll(generatedVars, unifier);
                } else {
                    newBodyCriticalEdges = newBodyCriticalEdges
                            .addAll(appliedCriticalEdges.retainAll(generatedVars, unifier), unifier);
                }
                newCriticalEdges = appliedCriticalEdges.removeAll(generatedVars, unifier);
            }
            // unsafeIntern : okay because the original rule body existential variables were added to FreshVars
            newBody = appliedBody.unsafeIntern(generatedVars, guard).withBodyCriticalEdges(newBodyCriticalEdges);
            diseq = Optional.of(Diseq.of(generatedVars, guard));
        }

        // construct result
        final ApplyResult applyResult = ApplyResult.of(diseq, newBody, newCriticalEdges);

        return Optional.of(applyResult);
    }

}