package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.task.NullProgress;
import org.metaborg.util.task.ThreadCancel;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.ImmutableList;

import mb.nabl2.config.NaBL2DebugConfig;
import mb.nabl2.constraints.Constraints;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.solver.Fresh;
import mb.nabl2.solver.ISolution;
import mb.nabl2.solver.SolverConfig;
import mb.nabl2.solver.exceptions.SolverException;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.solvers.BaseSolver.GraphSolution;
import mb.nabl2.solver.solvers.ImmutableBaseSolution;
import mb.nabl2.solver.solvers.SingleFileSolver;
import mb.nabl2.spoofax.analysis.IResult;
import mb.nabl2.spoofax.analysis.ImmutableSingleUnitResult;
import mb.nabl2.stratego.ConstraintTerms;
import mb.nabl2.stratego.MessageTerms;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.StrategoBlob;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.nabl2.terms.unification.Unifiers;

public class SG_solve_single_constraint extends AbstractPrimitive {

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

        final ICancel cancel = new ThreadCancel();
        final IProgress progress = new NullProgress();
        final SingleFileSolver solver = new SingleFileSolver(debugConfig,
                ScopeGraphMultiFileAnalysisPrimitive.callExternal(env, strategoTerms));
        final ISolution solution;
        try {
            GraphSolution graphSolution = solver.solveGraph(
                    ImmutableBaseSolution.of(solverConfig, constraints, Unifiers.Immutable.of()), fresh::fresh,
                    cancel, progress);
            ISolution constraintSolution = solver.solve(graphSolution, fresh::fresh, cancel, progress);
            solution = constraintSolution;
        } catch(InterruptedException | SolverException ex) {
            throw new InterpreterException(ex);
        }

        final IResult result = ImmutableSingleUnitResult.of(constraints, solution, Optional.empty(), fresh.freeze());
        final IMessages.Immutable messages = solution.messagesAndUnsolvedErrors();
        final IStrategoTerm errors =
                strategoTerms.toStratego(MessageTerms.toTerms(messages.getErrors(), solution.unifier()));
        final IStrategoTerm warnings =
                strategoTerms.toStratego(MessageTerms.toTerms(messages.getWarnings(), solution.unifier()));
        final IStrategoTerm notes =
                strategoTerms.toStratego(MessageTerms.toTerms(messages.getNotes(), solution.unifier()));
        final IStrategoTerm resultTerm = env.getFactory().makeTuple(new StrategoBlob(result), errors, warnings, notes);
        env.setCurrent(resultTerm);
        return true;
    }

}