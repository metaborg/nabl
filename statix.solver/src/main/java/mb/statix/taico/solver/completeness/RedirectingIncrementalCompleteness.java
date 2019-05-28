package mb.statix.taico.solver.completeness;

import java.util.Map;

import com.google.common.collect.Multiset;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.IncrementalCompleteness;
import mb.statix.spec.Spec;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.util.Scopes;

public class RedirectingIncrementalCompleteness extends IncrementalCompleteness {
    private final String owner;
    
    public RedirectingIncrementalCompleteness(String owner, Spec spec) {
        super(spec);
        this.owner = owner;
    }
    
    protected RedirectingIncrementalCompleteness(String owner, Spec spec, Map<ITerm, Multiset<ITerm>> incomplete) {
        super(spec, incomplete);
        this.owner = owner;
    }

    @Override
    public boolean isComplete(Scope scope, ITerm label, IUnifier unifier) {
        if (scope.getResource().equals(owner)) {
            return super.isComplete(scope, label, unifier);
        } else {
            return Scopes.getOwnerUnchecked(scope).getCurrentState().solver().getCompleteness().isComplete(scope, label, unifier);
        }
    }

    @Override
    public void add(IConstraint constraint, IUnifier unifier) {
        Completeness.criticalEdges(constraint, spec, (scopeTerm, label) -> {
            getVarOrScope(scopeTerm, unifier).ifPresent(scope -> {
                getTarget(scope).add(scope, label);
            });
        });
    }

    @Override
    public void remove(IConstraint constraint, IUnifier unifier) {
        Completeness.criticalEdges(constraint, spec, (scopeTerm, label) -> {
            getVarOrScope(scopeTerm, unifier).ifPresent(scope -> {
                getTarget(scope).remove(scope, label);
            });
        });
    }
    
    private RedirectingIncrementalCompleteness getTarget(ITerm term) {
        if (!(term instanceof Scope)) return this;
        
        final String owner = ((Scope) term).getResource();
        if (this.owner.equals(owner)) return this;
        
        return SolverContext.context().getModuleUnchecked(owner).getCurrentState().solver().getCompleteness();
    }
}
