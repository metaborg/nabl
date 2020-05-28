package mb.statix.solver.concurrent;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Spec;

public class StatixTypeChecker extends AbstractTypeChecker<Scope, ITerm, ITerm, SolverResult> {

    private final StatixSolver solver;

    public StatixTypeChecker(String resource, Coordinator<Scope, ITerm, ITerm> coordinator, Spec spec,
            IConstraint constraint, IDebugContext debug) {
        super(resource, coordinator);
        this.solver = new StatixSolver(spec, constraint, debug, this);
    }

    @Override public void start(Scope root) throws InterruptedException {
        started(io.usethesource.capsule.Set.Immutable.of());
        solver.solve().thenAccept(this::done);
    }

    @Override public void fail() {
    }

    @Override public String toString() {
        return "StatixTypeChecker[" + resource() + "]";
    }

}