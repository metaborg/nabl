package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Spec;

public abstract class StatixConstraintPrimitive extends StatixPrimitive {

    @Inject public StatixConstraintPrimitive(String name) {
        super(name, 4);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        final ITerm specTerm = terms.get(0);
        final Spec spec = StatixTerms.spec().match(specTerm)
                .orElseThrow(() -> new InterpreterException("Expected spec, got " + specTerm));
        reportOverlappingRules(spec);

        final IDebugContext debug = getDebugContext(terms.get(1));
        final IProgress progress = getProgress(terms.get(2));
        final ICancel cancel = getCancel(terms.get(3));

        final Function1<IConstraint, ITerm> solveConstraint =
                constraint -> solveConstraint(spec, constraint, debug, progress, cancel);
        // @formatter:off
        return M.cases(
            StatixTerms.constraint().map(solveConstraint::apply),
            M.listElems(StatixTerms.constraint()).map(vars_constraints -> {
                return B.newList(vars_constraints.stream().parallel().map(solveConstraint::apply).collect(ImmutableList.toImmutableList()));
            })
        ).match(term);
        // @formatter:on
    }

    private ITerm solveConstraint(Spec spec, IConstraint constraint, IDebugContext debug, IProgress progress,
            ICancel cancel) {
        final SolverResult resultConfig;

        try {
            resultConfig = solve(spec, constraint, debug, progress, cancel);
        } catch(InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        final IUniDisunifier.Immutable unifier = resultConfig.state().unifier();

        final List<ITerm> substEntries = Lists.newArrayList();
        for(Entry<ITermVar, ITermVar> e : resultConfig.existentials().entrySet()) {
            final ITerm v = StatixTerms.explode(e.getKey());
            final ITerm t = StatixTerms.explicateVars(unifier.findRecursive(e.getValue()));
            substEntries.add(B.newTuple(v, t));
        }

        final ITerm substTerm = B.newList(substEntries);
        final ITerm solverTerm = B.newBlob(resultConfig);
        final ITerm resultTerm = B.newAppl("Solution", substTerm, solverTerm);

        return resultTerm;
    }

    protected abstract SolverResult solve(Spec spec, IConstraint constraint, IDebugContext debug, IProgress progress,
            ICancel cancel) throws InterruptedException, ExecutionException;

}

