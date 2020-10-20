package mb.statix.concurrent.solver;

import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.ITypeChecker;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;
import mb.statix.spec.Spec;

public class UnitTypeChecker implements ITypeChecker<Scope, ITerm, ITerm, SolverResult> {

    private final Rule rule;
    private final Spec spec;
    private final IDebugContext debug;

    private StatixSolver solver;

    public UnitTypeChecker(Rule rule, Spec spec, IDebugContext debug) {
        this.rule = rule;
        this.spec = spec;
        this.debug = debug;
    }

    @Override public IFuture<SolverResult> run(ITypeCheckerContext<Scope, ITerm, ITerm, SolverResult> context,
            Scope root) {
        final IState.Immutable unitState = State.of(spec).withResource(context.id());
        final ApplyResult applyResult;
        if((applyResult = RuleUtil.apply(unitState, rule, ImmutableList.of(root), null).orElse(null)) == null) {
            return CompletableFuture
                    .completedExceptionally(new IllegalArgumentException("Cannot apply initial rule to root scope."));
        }
        solver = new StatixSolver(applyResult.body(), spec, applyResult.state(), Completeness.Immutable.of(spec), debug,
                new NullProgress(), new NullCancel(), context);
        return solver.solve(root);
    }

    @Override public IFuture<ITerm> getExternalDatum(ITerm datum) {
        return solver.getExternalRepresentation(datum);
    }

}