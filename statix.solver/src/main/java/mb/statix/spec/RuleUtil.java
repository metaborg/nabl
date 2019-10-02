package mb.statix.spec;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
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
        Function1<Optional<ITermVar>, ITermVar> fresh = v -> newState.freshVar(v.map(ITermVar::getName).orElse("wld"));
        return P.matchWithEqs(rule.params(), args, state.unifier(), fresh).flatMap(matchResult -> {
            final IConstraint newConstraint = rule.body().apply(matchResult.substitution()).withCause(cause);
            final ApplyResult applyResult;
            if(matchResult.constrainedVars().isEmpty()) {
                applyResult = ApplyResult.of(newState.freeze(), ImmutableSet.of(), ImmutableMap.of(), newConstraint);
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
                final Map<ITermVar, ITerm> guard =
                        diff.retainAll(matchResult.constrainedVars()).unifier().equalityMap();

                // construct result
                final IState.Immutable resultState = newState.freeze().withUnifier(newUnifier);
                applyResult = ApplyResult.of(resultState, diff.varSet(), guard, newConstraint);
            }
            return Optional.of(applyResult);
        });
    }

}