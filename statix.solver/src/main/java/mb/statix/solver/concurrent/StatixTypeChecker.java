package mb.statix.solver.concurrent;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;

public class StatixTypeChecker extends AbstractTypeChecker<Scope, ITerm, ITerm> {

    private final Spec spec;
    private final IConstraint constraint;

    public StatixTypeChecker(String resource, Coordinator<Scope, ITerm, ITerm> coordinator, Spec spec,
            IConstraint constraint, IDebugContext debug) {
        super(resource, coordinator);
        this.spec = spec;
        this.constraint = constraint;
    }

    @Override public void start(Scope root) {
    }

    @Override public void fail() {
    }

}