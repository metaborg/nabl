package mb.statix.constraints;

import java.io.Serializable;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.util.Vars;

/**
 * Implementation for a tell edge constraint.
 * 
 * <pre>sourceScope -label-> targetScope</pre>
 */
public class CTellEdge implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

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

    public ITerm sourceTerm() {
        return sourceTerm;
    }

    public ITerm label() {
        return label;
    }

    public ITerm targetTerm() {
        return targetTerm;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTellEdge withCause(@Nullable IConstraint cause) {
        return new CTellEdge(sourceTerm, label, targetTerm, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTellEdge(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTellEdge(this);
    }

    @Override public CTellEdge apply(ISubstitution.Immutable subst) {
        return new CTellEdge(subst.apply(sourceTerm), label, subst.apply(targetTerm), cause);
    }
    
    @Override
    public Optional<MConstraintResult> solve(IMState state, MConstraintContext params) throws Delay {
        //Modifies the scope graph
        final IUnifier.Immutable unifier = state.unifier();
        if(!unifier.isGround(sourceTerm)) {
            throw Delay.ofVars(unifier.getVars(sourceTerm));
        }
        if(!unifier.isGround(targetTerm)) {
            throw Delay.ofVars(unifier.getVars(targetTerm));
        }
        final Scope source = Scope.matcher().match(sourceTerm, unifier).orElseThrow(
                () -> new IllegalArgumentException("Expected source scope, got " + unifier.toString(sourceTerm)));
        if(params.isClosed(source, state)) {
            return Optional.empty();
        }
        final Scope target = Scope.matcher().match(targetTerm, unifier).orElseThrow(
                () -> new IllegalArgumentException("Expected target scope, got " + unifier.toString(targetTerm)));
        state.scopeGraph().addEdge(source, label, target);
        return Optional.of(new MConstraintResult());
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