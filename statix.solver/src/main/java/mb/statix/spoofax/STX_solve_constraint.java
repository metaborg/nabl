package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.Level;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.Completeness;
import mb.statix.solver.Config;
import mb.statix.solver.IConstraint;
import mb.statix.solver.LoggerDebugContext;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.spec.Spec;

public class STX_solve_constraint extends StatixPrimitive {
    private static final ILogger logger = LoggerUtils.logger(STX_solve_constraint.class);

    @Inject public STX_solve_constraint() {
        super(STX_solve_constraint.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        final Spec spec =
                StatixTerms.spec().match(terms.get(0)).orElseThrow(() -> new InterpreterException("Expected spec."));

        final Level level = M.stringValue().match(terms.get(1)).map(Level::parse)
                .orElseThrow(() -> new InterpreterException("Expected log level."));

        final Tuple2<List<ITermVar>, Set<IConstraint>> vars_constraint = M
                .tuple2(M.listElems(StatixTerms.var()), StatixTerms.constraints(spec.labels()),
                        (t, vs, c) -> ImmutableTuple2.of(vs, c))
                .match(term).orElseThrow(() -> new InterpreterException("Expected constraint."));

        State state = State.of(spec);
        final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
        for(ITermVar var : vars_constraint._1()) {
            final Tuple2<ITermVar, State> var_state = state.freshVar(var.getName());
            state = var_state._2();
            subst.put(var, var_state._1());
            subst.put(var_state._1(), var);
        }
        final ISubstitution.Immutable isubst = subst.freeze();
        final Set<IConstraint> constraints =
                vars_constraint._2().stream().map(c -> c.apply(isubst)).collect(Collectors.toSet());
        final Config config = Config.of(state, constraints, new Completeness());
        final Config resultConfig;
        try {
            resultConfig = Solver.solve(config, new LoggerDebugContext(logger, level));
        } catch(InterruptedException e) {
            throw new InterpreterException(e);
        }

        final ITerm ast = B.EMPTY_TUPLE;
        final List<ITerm> errorList = Lists.newArrayList();
        if(resultConfig.hasErrors()) {
            errorList.add(B.newTuple(ast, B.newString(resultConfig.errors().size() + " error(s).")));
        }

        final State resultState = resultConfig.state();
        final Collection<IConstraint> unsolved = resultConfig.constraints();
        if(!unsolved.isEmpty()) {
            logger.warn("Unsolved constraints: {}",
                    unsolved.stream().map(c -> c.toString(resultState.unifier())).collect(Collectors.toList()));
            errorList.add(B.newTuple(ast, B.newString(unsolved.size() + " unsolved constraint(s).")));
        }

        final IUnifier unifier = resultState.unifier();
        List<ITerm> vsubst = Lists.newArrayList();
        for(ITermVar var : vars_constraint._1()) {
            final ITerm key = isubst.apply(var);
            final ITerm value = unifier.findRecursive(key);
            final ITerm varTerm = isubst.apply(value);
            if(!var.equals(varTerm)) {
                vsubst.add(B.newTuple(StatixTerms.explicate(var), StatixTerms.explicate(varTerm)));
            }
        }
        final ITerm solution = B.newList(vsubst);
        final IListTerm errors = B.newList(errorList);
        final IListTerm warnings = B.EMPTY_LIST;
        final IListTerm notes = B.EMPTY_LIST;
        final ITerm resultTerm = B.newTuple(solution, errors, warnings, notes);
        return Optional.of(resultTerm);
    }

}