package mb.statix.spoofax;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Spec;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static mb.nabl2.terms.build.TermBuild.B;

public class STX_solve_constraint_with_initial_state extends StatixPrimitive {

    @Inject
    public STX_solve_constraint_with_initial_state() {
        super(STX_solve_constraint_with_initial_state.class.getSimpleName(), 4);
    }

    @Override
    protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms) throws InterpreterException {
        final IConstraint constraint = StatixTerms.constraint().match(term)
            .orElseThrow(() -> new InterpreterException("Expected constraint, got " + term));
        final SolverResult<?> solverResult = getResult(terms.get(0));
        final Spec spec = solverResult.spec();
        final IDebugContext debug = getDebugContext(terms.get(1));
        final IProgress progress = getProgress(terms.get(2));
        final ICancel cancel = getCancel(terms.get(3));
        final SolverResult<?> resultConfig;

        try {
            resultConfig = Solver.solve(spec, solverResult.state(), constraint, debug, cancel, progress, 0);
        } catch(InterruptedException e) {
            throw new InterpreterException(e);
        }
        final IUniDisunifier.Immutable unifier = resultConfig.state().unifier();

        final List<ITerm> substEntries = new ArrayList<>();
        for(Map.Entry<ITermVar, ITermVar> e : resultConfig.existentials().entrySet()) {
            final ITerm v = StatixTerms.explode(e.getKey());
            final ITerm t = StatixTerms.explicateVars(unifier.findRecursive(e.getValue()));
            substEntries.add(B.newTuple(v, t));
        }

        final ITerm substTerm = B.newList(substEntries);
        final ITerm solverTerm = B.newBlob(resultConfig);
        final ITerm resultTerm = B.newAppl("Solution", substTerm, solverTerm);

        return Optional.of(resultTerm);
    }
}
