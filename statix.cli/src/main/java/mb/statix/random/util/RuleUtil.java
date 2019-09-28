package mb.statix.random.util;

import java.util.List;
import java.util.Optional;

import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
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
        // FIXME The current method can cause capture if a solver-generated variable
        //       has the same name as a pattern variable in the rule. The arguments
        //       contain solver variables, but they are substituted under an exists
        //       with the literal rule pattern variables. To do this correctly,
        //       we must rename pattern variables that might clash. It probably works because
        //       the variable naming convention in the solver generates names that are
        //       disallowed in specs.

        // create equality constraints
        final List<CEqual> patternEqs = Lists.newArrayList();
        final List<CEqual> paramEqs = ImmutableList.copyOf(Iterables2.zip(rule.params(), args, (param, arg) -> {
            Tuple2<ITerm, Multimap<ITermVar, ITerm>> termAndEqs = param.asTerm();
            termAndEqs._2().forEach((t1, t2) -> {
                patternEqs.add(new CEqual(t1, t2, cause));
            });
            return new CEqual(termAndEqs._1(), arg, cause);
        }));

        // solve the resulting existential, but without (!) the rule body
        final IConstraint constraint =
                new CExists(rule.paramVars(), Constraints.conjoin(Iterables.concat(patternEqs, paramEqs)), cause);
        SolverResult result;
        try {
            result = Solver.solve(state, constraint, new NullDebugContext());
        } catch(InterruptedException e) {
            throw new MetaborgRuntimeException(e);
        }
        if(result.hasErrors() || !result.delays().isEmpty()) {
            return Optional.empty();
        }

        // apply the existential to the body constraint
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(result.existentials());
        final IConstraint newConstraint = rule.body().apply(subst);

        return Optional.of(ImmutableTuple2.of(result, newConstraint));
    }

}