package mb.statix.spec;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.MatchResult;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.u.PersistentUnifier;
import mb.nabl2.terms.unification.ud.Diseq;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.VoidException;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;

class ApplyRelaxed extends ApplyMode<VoidException> {

    @Override Optional<ApplyResult> apply(IState.Immutable state, Rule rule, List<? extends ITerm> args,
            IConstraint cause) throws VoidException {
        // create equality constraints
        final IState.Transient newState = state.melt();
        final Set.Transient<ITermVar> _universalVars = Set.Transient.of();
        final Function1<Optional<ITermVar>, ITermVar> fresh = v -> {
            final ITermVar f = v.map(newState::freshVar).orElseGet(newState::freshWld);
            _universalVars.__insert(f);
            return f;
        };
        final MatchResult matchResult;
        if((matchResult = P.matchWithEqs(rule.params(), args, state.unifier(), fresh).orElse(null)) == null) {
            return Optional.empty();
        }

        final Set.Immutable<ITermVar> universalVars = _universalVars.freeze();
        final SetView<ITermVar> constrainedVars = Sets.difference(matchResult.constrainedVars(), universalVars);
        final IConstraint newConstraint = rule.body().apply(matchResult.substitution()).withCause(cause);
        final ICompleteness.Immutable newBodyCriticalEdges =
                rule.bodyCriticalEdges() == null ? null : rule.bodyCriticalEdges().apply(matchResult.substitution());

        final ApplyResult applyResult;
        if(constrainedVars.isEmpty()) {
            applyResult = ApplyResult.of(newState.freeze(), PersistentUnifier.Immutable.of(), Optional.empty(),
                    newConstraint, newBodyCriticalEdges);
        } else {
            // simplify guard constraints
            final IUniDisunifier.Result<IUnifier.Immutable> unifyResult;
            try {
                if((unifyResult = state.unifier().unify(matchResult.equalities()).orElse(null)) == null) {
                    return Optional.empty();
                }
            } catch(OccursException e) {
                return Optional.empty();
            }
            final IUniDisunifier.Immutable newUnifier = unifyResult.unifier();
            final IUnifier.Immutable diff = unifyResult.result();

            // construct guard
            final IUnifier.Immutable guard = diff.retainAll(constrainedVars).unifier();
            if(guard.isEmpty()) {
                throw new IllegalStateException("Guard not expected to be empty here.");
            }
            final Diseq diseq = Diseq.of(universalVars, guard);

            // construct result
            final IState.Immutable resultState = newState.freeze().withUnifier(newUnifier);
            applyResult = ApplyResult.of(resultState, diff, Optional.of(diseq), newConstraint, newBodyCriticalEdges);
        }
        return Optional.of(applyResult);
    }

}