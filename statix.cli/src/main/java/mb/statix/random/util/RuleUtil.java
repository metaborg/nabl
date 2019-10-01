package mb.statix.random.util;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.core.MetaborgRuntimeException;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Rule;

public class RuleUtil {

    public static final Optional<Tuple2<SolverResult, IConstraint>> apply(IState.Immutable state, Rule rule,
            List<ITerm> args, IConstraint cause) {
        // create equality constraints
        final IState.Transient newState = state.melt();
        return P.newTuple(rule.params())
                .matchWithEqs(B.newTuple(args), state.unifier(), (v) -> newState.freshVar(v.getName()))
                .flatMap(matchResult -> {
                    // build constraint
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
                        throw new MetaborgRuntimeException(e);
                    }
                    if(solveResult.hasErrors() || !solveResult.delays().isEmpty()) {
                        return Optional.empty();
                    }

                    // apply the substitution to the body constraint
                    final IConstraint newConstraint = rule.body().apply(matchResult.substitution());

                    return Optional.of(ImmutableTuple2.of(solveResult, newConstraint));
                });
    }

}