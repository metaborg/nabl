package mb.statix.solver.constraint;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.ConstraintResult;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;
import mb.statix.spec.Spec;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.scopegraph.OwnableScope;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.MState;

/**
 * Implementation for a tell edge constraint.
 * 
 * <pre>sourceScope -label-> targetScope</pre>
 */
public class CTellEdge implements IConstraint {

    private final ITerm sourceTerm;
    private final ITerm label;
    private final ITerm targetTerm;

    private final @Nullable IConstraint cause;

    /**
     * Creates a new tell edge constraint with the given source, label and target.
     * 
     * @param sourceTerm
     *      the term representing the source scope
     * @param label
     *      the label of the edge
     * @param targetTerm
     *      the term representing the target scope
     */
    public CTellEdge(ITerm sourceTerm, ITerm label, ITerm targetTerm) {
        this(sourceTerm, label, targetTerm, null);
    }

    public CTellEdge(ITerm sourceTerm, ITerm label, ITerm targetTerm, @Nullable IConstraint cause) {
        this.sourceTerm = sourceTerm;
        this.label = label;
        this.targetTerm = targetTerm;
        this.cause = cause;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTellEdge withCause(@Nullable IConstraint cause) {
        return new CTellEdge(sourceTerm, label, targetTerm, cause);
    }

    @Override public Collection<CriticalEdge> criticalEdges(Spec spec) {
        return ImmutableList.of(CriticalEdge.of(sourceTerm, label));
    }

    @Override public CTellEdge apply(ISubstitution.Immutable subst) {
        return new CTellEdge(subst.apply(sourceTerm), label, subst.apply(targetTerm), cause);
    }

    /**
     * @see IConstraint#solve
     * 
     * @throws IllegalArgumentException
     *      If the source or target is not a scope.
     * @throws Delay
     *      If the source or target is not ground.
     */
    @Override public Optional<ConstraintResult> solve(State state, ConstraintContext params) throws Delay {
        final IUnifier.Immutable unifier = state.unifier();
        if(!unifier.isGround(sourceTerm)) {
            throw Delay.ofVars(unifier.getVars(sourceTerm));
        }
        if(!unifier.isGround(targetTerm)) {
            throw Delay.ofVars(unifier.getVars(targetTerm));
        }
        final Scope source = Scope.matcher().match(sourceTerm, unifier).orElseThrow(
                () -> new IllegalArgumentException("Expected source scope, got " + unifier.toString(sourceTerm)));
        if(params.isClosed(source)) {
            return Optional.empty();
        }
        final Scope target = Scope.matcher().match(targetTerm, unifier).orElseThrow(
                () -> new IllegalArgumentException("Expected target scope, got " + unifier.toString(targetTerm)));
        final IScopeGraph.Immutable<ITerm, ITerm, ITerm> scopeGraph = state.scopeGraph().addEdge(source, label, target);
        return Optional.of(ConstraintResult.of(state.withScopeGraph(scopeGraph)));
    }
    
    @Override
    public Optional<MConstraintResult> solveMutable(MState state, ConstraintContext params) throws Delay {
        //Modifies the scope graph
        final IUnifier.Immutable unifier = state.unifier();
        if(!unifier.isGround(sourceTerm)) {
            throw Delay.ofVars(unifier.getVars(sourceTerm));
        }
        if(!unifier.isGround(targetTerm)) {
            throw Delay.ofVars(unifier.getVars(targetTerm));
        }
        final OwnableScope source = OwnableScope.ownableMatcher(ModuleManager::getModule).match(sourceTerm, unifier).orElseThrow(
                () -> new IllegalArgumentException("Expected source scope, got " + unifier.toString(sourceTerm)));
        if(params.isClosed(source)) {
            return Optional.empty();
        }
        final OwnableScope target = OwnableScope.ownableMatcher(ModuleManager::getModule).match(targetTerm, unifier).orElseThrow(
                () -> new IllegalArgumentException("Expected target scope, got " + unifier.toString(targetTerm)));
        state.scopeGraph().addEdge(source, label, target);
        return Optional.of(new MConstraintResult(state));
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(termToString.format(sourceTerm));
        sb.append(" -");
        sb.append(termToString.format(label));
        sb.append("-> ");
        sb.append(termToString.format(targetTerm));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}