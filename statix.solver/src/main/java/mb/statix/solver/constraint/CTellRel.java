package mb.statix.solver.constraint;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import mb.statix.spec.Type;

/**
 * Implementation for a tell relation constraint.
 * 
 * <pre>scope -relation->[] data</pre>
 */
public class CTellRel implements IConstraint {

    private final ITerm scopeTerm;
    private final ITerm relation;
    private final List<ITerm> datumTerms;

    private final @Nullable IConstraint cause;

    /**
     * Creates a new tell relation constraint without a cause.
     * 
     * @param scopeTerm
     *      the scope to add the relation to
     * @param relation
     *      the relation to add
     * @param datumTerms
     *      the data to add on the relation
     */
    public CTellRel(ITerm scopeTerm, ITerm relation, Iterable<ITerm> datumTerms) {
        this(scopeTerm, relation, datumTerms, null);
    }

    /**
     * Creates a new tell relation constraint with a cause.
     * 
     * @param scopeTerm
     *      the scope to add the relation to
     * @param relation
     *      the relation to add
     * @param datumTerms
     *      the data to add on the relation
     * @param cause
     *      the constraint that caused this constraint to be created
     */
    public CTellRel(ITerm scopeTerm, ITerm relation, Iterable<ITerm> datumTerms, @Nullable IConstraint cause) {
        this.scopeTerm = scopeTerm;
        this.relation = relation;
        this.datumTerms = ImmutableList.copyOf(datumTerms);
        this.cause = cause;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTellRel withCause(@Nullable IConstraint cause) {
        return new CTellRel(scopeTerm, relation, datumTerms, cause);
    }

    @Override public Collection<CriticalEdge> criticalEdges(Spec spec) {
        return ImmutableList.of(CriticalEdge.of(scopeTerm, relation));
    }

    @Override public CTellRel apply(ISubstitution.Immutable subst) {
        return new CTellRel(subst.apply(scopeTerm), relation, subst.apply(datumTerms));
    }

    /**
     * @see IConstraint#solve
     * 
     * @throws IllegalArgumentException
     *      If the relation is being added to a term that is not a scope.
     * @throws Delay
     *      If the scope we are querying is not ground relative to the unifier.
     */
    @Override public Optional<ConstraintResult> solve(State state, ConstraintContext params) throws Delay {
        final Type type = state.spec().relations().get(relation);
        if(type == null) {
            params.debug().error("Ignoring data for unknown relation {}", relation);
            return Optional.empty();
        }
        if(type.getArity() != datumTerms.size()) {
            params.debug().error("Ignoring {}-ary data for {}-ary relation {}", datumTerms.size(), type.getArity(),
                    relation);
            return Optional.empty();
        }

        final IUnifier.Immutable unifier = state.unifier();
        if(!unifier.isGround(scopeTerm)) {
            throw Delay.ofVars(unifier.getVars(scopeTerm));
        }
        final Scope scope = Scope.matcher().match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));
        if(params.isClosed(scope)) {
            return Optional.empty();
        }

        final ITerm key = B.newTuple(datumTerms.stream().limit(type.getInputArity()).collect(Collectors.toList()));
        if(!unifier.isGround(key)) {
            throw Delay.ofVars(unifier.getVars(key));
        }
        Optional<ITerm> existingValue = state.scopeGraph().getData().get(scope, relation).stream().filter(dt -> {
            return unifier
                    .areEqual(key, B.newTuple(dt.stream().limit(type.getInputArity()).collect(Collectors.toList())))
                    .orElse(false);
        }).findFirst().map(dt -> {
            return B.newTuple(dt.stream().skip(type.getInputArity()).collect(Collectors.toList()));
        });
        if(existingValue.isPresent()) {
            final ITerm value = B.newTuple(datumTerms.stream().skip(type.getInputArity()).collect(Collectors.toList()));
            return Optional.of(ConstraintResult.ofConstraints(state, new CEqual(value, existingValue.get(), this)));
        } else {
            final IScopeGraph.Immutable<ITerm, ITerm, ITerm> scopeGraph =
                    state.scopeGraph().addDatum(scope, relation, datumTerms);
            return Optional.of(ConstraintResult.of(state.withScopeGraph(scopeGraph)));
        }
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(termToString.format(scopeTerm));
        sb.append(" -");
        sb.append(termToString.format(relation));
        sb.append("-[] ");
        sb.append(termToString.format(B.newTuple(datumTerms)));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}