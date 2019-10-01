package mb.statix.spec;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;

public class RuleUtil {

    public static final Optional<ApplyResult> apply(IState.Immutable state, Rule rule, List<ITerm> args,
            @Nullable IConstraint cause) {
        // create equality constraints
        final IState.Transient newState = state.melt();
        Function1<Optional<ITermVar>, ITermVar> fresh = v -> newState.freshVar(v.map(ITermVar::getName).orElse("wld)"));
        return P.matchWithEqs(rule.params(), args, state.unifier(), fresh).flatMap(matchResult -> {
            final IConstraint newConstraint = rule.body().apply(matchResult.substitution()).withCause(cause);
            final ApplyResult applyResult;
            if(matchResult.constrainedVars().isEmpty()) {
                applyResult = ApplyResult.of(newState.freeze(), ImmutableSet.of(), matchResult.constrainedVars(),
                        newConstraint);
            } else {
                // build equality constraint
                final List<CEqual> eqs = matchResult.equalities().stream()
                        .map(eq -> new CEqual(eq._1(), eq._2(), cause)).collect(Collectors.toList());
                final IConstraint constraint = Constraints.conjoin(eqs);

                // solve the resulting constraint, but without (!) the rule body, in the updated state
                // NOTE Simply adding the equalities to the rule body breaks the important invariant
                //      that scope variables are either ground scopes, or will be instantiated to fresh scopes
                //      that do not yet exist. However, the equality ?s == #s1 breaks this, since ?s is still
                //      free until this constraint is solved, but it will be instantiated to a scope that already
                //      exists.
                SolverResult solveResult;
                try {
                    solveResult = Solver.solve(newState.freeze(), constraint, new NullDebugContext());
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(solveResult.hasErrors() || !solveResult.delays().isEmpty()) {
                    return Optional.empty();
                }
                applyResult = ApplyResult.of(solveResult.state(), solveResult.updatedVars(),
                        matchResult.constrainedVars(), newConstraint);
            }
            return Optional.of(applyResult);
        });
    }

}