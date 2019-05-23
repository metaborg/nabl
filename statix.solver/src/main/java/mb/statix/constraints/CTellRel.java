package mb.statix.constraints;

import static mb.nabl2.terms.build.TermBuild.B;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;

/**
 * Implementation for a tell relation constraint.
 * 
 * <pre>scope -relation->[] data</pre>
 */
public class CTellRel implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

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

    public ITerm scopeTerm() {
        return scopeTerm;
    }

    public ITerm relation() {
        return relation;
    }

    public List<ITerm> datumTerms() {
        return datumTerms;
    }

    public ITerm datumTerm() {
        return B.newTuple(datumTerms);
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTellRel withCause(@Nullable IConstraint cause) {
        return new CTellRel(scopeTerm, relation, datumTerms, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTellRel(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTellRel(this);
    }

    @Override public CTellRel apply(ISubstitution.Immutable subst) {
        return new CTellRel(subst.apply(scopeTerm), relation, subst.apply(datumTerms));
    }
    
    @Override
    public Optional<MConstraintResult> solve(IMState state, MConstraintContext params) throws Delay {
        final ITerm scopeTerm = scopeTerm();
        final ITerm relation = relation();
        final ITerm datumTerm = datumTerm();

        final IUnifier unifier = state.unifier();
        if(!unifier.isGround(scopeTerm)) {
            throw Delay.ofVars(unifier.getVars(scopeTerm));
        }
        final Scope scope = AScope.matcher().match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));
        if(params.isClosed(scope, state)) {
            return Optional.empty();
        }

        state.scopeGraph().addDatum(scope, relation, datumTerm);
        return Optional.of(MConstraintResult.of());
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