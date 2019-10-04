package mb.statix.spec;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.Diseq;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.IUnifier.Immutable.Result;
import mb.nabl2.terms.unification.OccursException;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;

public class RuleUtil {

    public static final Optional<ApplyResult> apply(IState.Immutable state, Rule rule, List<ITerm> args,
            @Nullable IConstraint cause) {
        // create equality constraints
        final IState.Transient newState = state.melt();
        final Set.Transient<ITermVar> _universalVars = Set.Transient.of();
        final Function1<Optional<ITermVar>, ITermVar> fresh = v -> {
            final ITermVar f;
            if(v.isPresent()) {
                f = newState.freshVar(v.get().getName());
            } else {
                f = newState.freshVar("_");
                _universalVars.__insert(f);
            }
            return f;
        };
        return P.matchWithEqs(rule.params(), args, state.unifier(), fresh).flatMap(matchResult -> {
            final Set.Immutable<ITermVar> universalVars = _universalVars.freeze();
            final SetView<ITermVar> constrainedVars = Sets.difference(matchResult.constrainedVars(), universalVars);
            final IConstraint newConstraint = rule.body().apply(matchResult.substitution()).withCause(cause);

            final ApplyResult applyResult;
            if(constrainedVars.isEmpty()) {
                applyResult = ApplyResult.of(newState.freeze(), ImmutableSet.of(), Optional.empty(), newConstraint);
            } else {
                // simplify guard constraints
                final Result<IUnifier.Immutable> unifyResult;
                try {
                    if((unifyResult = state.unifier().unify(matchResult.equalities()).orElse(null)) == null) {
                        return Optional.empty();
                    }
                } catch(OccursException e) {
                    return Optional.empty();
                }
                final IUnifier.Immutable newUnifier = unifyResult.unifier();
                final IUnifier.Immutable diff = unifyResult.result();

                // construct guard
                final Map<ITermVar, ITerm> guard = diff.retainAll(constrainedVars).unifier().equalityMap();
                final Diseq diseq = new Diseq(universalVars, guard);

                // construct result
                final IState.Immutable resultState = newState.freeze().withUnifier(newUnifier);
                applyResult = ApplyResult.of(resultState, diff.varSet(), Optional.of(diseq), newConstraint);
            }
            return Optional.of(applyResult);
        });
    }

}