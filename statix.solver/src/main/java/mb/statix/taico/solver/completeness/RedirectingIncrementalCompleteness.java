package mb.statix.taico.solver.completeness;

import java.util.Map;

import com.google.common.collect.Multiset;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
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
    
    /**
     * @param term
     *      the term
     * 
     * @return
     *      the completeness to which the request for the given term should be redirected
     */
    private RedirectingIncrementalCompleteness getTarget(ITerm term) {
        final String owner;
        if (term instanceof ITermVar) {
            owner = ((ITermVar) term).getResource();
        } else if (term instanceof Scope) {
            owner = ((Scope) term).getResource();
        } else {
            throw new IllegalArgumentException("Expected variable or scope, but was " + term);
        }
        
        if (this.owner.equals(owner)) return this;
        
        return SolverContext.context().getModuleUnchecked(owner).getCurrentState().solver().getCompleteness();
    }
}
