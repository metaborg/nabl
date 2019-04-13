package mb.statix.solver.constraint;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;
import mb.statix.spec.Type;
import mb.statix.taico.scopegraph.OwnableScope;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.MState;

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
        return ImmutableList.of(CriticalEdge.of(scopeTerm, relation, null));
    }

    @Override public CTellRel apply(ISubstitution.Immutable subst) {
        return new CTellRel(subst.apply(scopeTerm), relation, subst.apply(datumTerms));
    }
    
    @Override
    public boolean canModifyState() {
        return true;
    }

    /**
     * @see IConstraint#solve
     * 
     * @throws IllegalArgumentException
     *      If the relation is being added to a term that is not a scope.
     * @throws Delay
     *      If the scope we are querying is not ground relative to the unifier.
     */
    @Override
    public Optional<MConstraintResult> solve(MState state, MConstraintContext params) throws Delay {
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
        final OwnableScope scope = OwnableScope.ownableMatcher(state.manager()::getModule).match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));
        if(params.isClosed(scope)) {
            return Optional.empty();
        }
        
        //This checks if the type is of the form T * ... * T -> U * ... * U
        //If the scope is shared, it is not allowed
        //TODO Determine from the spec if this scope is shared, use that as condition
//        if (type.getOutputArity() > 0 && state.scopeGraph().getExtensibleScopes().contains(scope)) {
//            throw new IllegalStateException("Cross module unification of types is not allowed! " + type);
//        }

        final ITerm key = B.newTuple(datumTerms.stream().limit(type.getInputArity()).collect(Collectors.toList()));
        if(!unifier.isGround(key)) {
            throw Delay.ofVars(unifier.getVars(key));
        }
        
        state.scopeGraph().addDatum(scope, relation, datumTerms);
        
        //TODO The old behavior was to check if this data is equal (perform unification on it). This is now only allowed for a single module

//        LockManager lockManager = new LockManager(state.owner(), this);
//        List<ITerm> existingValues;
//        try {
//            existingValues = state.scopeGraph().getData(scope, relation, lockManager).stream().filter(dt -> {
//                return unifier
//                        .areEqual(key, B.newTuple(dt.getTarget().stream().limit(type.getInputArity()).collect(Collectors.toList())))
//                        .orElse(false);
//            }).map(dt -> {
//                return B.newTuple(dt.getTarget().stream().skip(type.getInputArity()).collect(Collectors.toList()));
//            }).collect(Collectors.toList());
//        } finally {
//            lockManager.releaseAll();
//        }
//        
//        //Ensure all existing are equal (unified to be equal)
//        if(!existingValues.isEmpty()) {
//            final ITerm value = B.newTuple(datumTerms.stream().skip(type.getInputArity()).collect(Collectors.toList()));
//            
//            int i = 0;
//            IConstraint[] constraints = new IConstraint[existingValues.size()];
//            for (ITerm existingValue : existingValues) {
//                constraints[i++] = new CEqual(value, existingValue, this);
//            }
//            
//            return Optional.of(new MConstraintResult(state, constraints));
//        } else {
            return Optional.of(new MConstraintResult(state));
//        }
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