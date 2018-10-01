package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.SDefT;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.ImmutableList;

import mb.nabl2.config.NaBL2DebugConfig;
import mb.nabl2.constraints.Constraints;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.solver.Fresh;
import mb.nabl2.solver.ISolution;
import mb.nabl2.solver.SolverConfig;
import mb.nabl2.solver.SolverException;
import mb.nabl2.solver.solvers.BaseSolver.GraphSolution;
import mb.nabl2.solver.solvers.CallExternal;
import mb.nabl2.solver.solvers.ImmutableBaseSolution;
import mb.nabl2.solver.solvers.SingleFileSolver;
import mb.nabl2.spoofax.analysis.IResult;
import mb.nabl2.spoofax.analysis.ImmutableSingleUnitResult;
import mb.nabl2.stratego.ConstraintTerms;
import mb.nabl2.stratego.MessageTerms;
import mb.nabl2.stratego.StrategoBlob;
import mb.nabl2.stratego.StrategoTerms;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.PersistentUnifier;

public class SG_solve_single_constraint extends AbstractPrimitive {

    private static ILogger logger = LoggerUtils.logger(SG_solve_single_constraint.class);

    public SG_solve_single_constraint() {
        super(SG_solve_single_constraint.class.getSimpleName(), 0, 1);
    }

    @Override public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars) throws InterpreterException {
        final StrategoTerms strategoTerms = new StrategoTerms(env.getFactory());

        final IStrategoTerm configSTerm = tvars[0];
        final ITerm configTerm = ConstraintTerms.specialize(strategoTerms.fromStratego(configSTerm));
        final SolverConfig solverConfig = SolverConfig.matcher().match(configTerm)
                .orElseThrow(() -> new InterpreterException("Term argument is not a solver config."));


        final ITerm constraintTerm = ConstraintTerms.specialize(strategoTerms.fromStratego(env.current()));
        final List<IConstraint> constraints = Constraints.matchConstraintOrList().map(ImmutableList::of)
                .match(constraintTerm).orElseThrow(() -> new InterpreterException("Current term is not a constraint."));

        NaBL2DebugConfig debugConfig = NaBL2DebugConfig.NONE; // FIXME How to get debug configuration in here?
        final Fresh.Transient fresh = Fresh.Transient.of();

        final ICancel cancel = new NullCancel();
        final IProgress progress = new NullProgress();
        final SingleFileSolver solver = new SingleFileSolver(debugConfig, callExternal(env, strategoTerms));
        final ISolution solution;
        try {
            GraphSolution graphSolution = solver.solveGraph(
                    ImmutableBaseSolution.of(solverConfig, constraints, PersistentUnifier.Immutable.of()), fresh::fresh,
                    cancel, progress);
            graphSolution = solver.reportUnsolvedGraphConstraints(graphSolution);
            ISolution constraintSolution = solver.solve(graphSolution, fresh::fresh, cancel, progress);
            constraintSolution = solver.reportUnsolvedConstraints(constraintSolution);
            solution = constraintSolution;
        } catch(InterruptedException | SolverException ex) {
            throw new InterpreterException(ex);
        }

        final IResult result = ImmutableSingleUnitResult.of(constraints, solution, Optional.empty(), fresh.freeze());
        final IStrategoTerm errors =
                strategoTerms.toStratego(MessageTerms.toTerms(solution.messages().getErrors(), solution.unifier()));
        final IStrategoTerm warnings =
                strategoTerms.toStratego(MessageTerms.toTerms(solution.messages().getWarnings(), solution.unifier()));
        final IStrategoTerm notes =
                strategoTerms.toStratego(MessageTerms.toTerms(solution.messages().getNotes(), solution.unifier()));
        final IStrategoTerm resultTerm = env.getFactory().makeTuple(new StrategoBlob(result), errors, warnings, notes);
        env.setCurrent(resultTerm);
        return true;
    }

    private CallExternal callExternal(IContext env, StrategoTerms strategoTerms) {
        return (name, args) -> {
            final IStrategoTerm[] sargs = Iterables2.stream(args).map(strategoTerms::toStratego)
                    .collect(Collectors.toList()).toArray(new IStrategoTerm[0]);
            final IStrategoTerm sarg = sargs.length == 1 ? sargs[0] : env.getFactory().makeTuple(sargs);
            final IStrategoTerm prev = env.current();
            try {
                env.setCurrent(sarg);
                final SDefT s = env.lookupSVar(name.replace("-", "_") + "_0_0");
                if(!s.evaluate(env)) {
                    return Optional.empty();
                }
                return Optional.ofNullable(env.current()).map(strategoTerms::fromStratego)
                        .map(ConstraintTerms::specialize);
            } catch(Exception ex) {
                logger.warn("External call to '{}' failed.", ex, name);
                return Optional.empty();
            } finally {
                env.setCurrent(prev);
            }
        };

    }

}