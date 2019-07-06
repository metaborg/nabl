package mb.statix.constraints;

import java.io.Serializable;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.stratego.TermOrigin;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.state.IMState;

public class CAstId implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm term;
    private final ITerm idTerm;

    private final @Nullable IConstraint cause;

    public CAstId(ITerm term, ITerm idTerm) {
        this(term, idTerm, null);
    }

    public CAstId(ITerm term, ITerm idTerm, @Nullable IConstraint cause) {
        this.term = term;
        this.idTerm = idTerm;
        this.cause = cause;
    }

    public ITerm astTerm() {
        return term;
    }

    public ITerm idTerm() {
        return idTerm;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CAstId withCause(@Nullable IConstraint cause) {
        return new CAstId(term, idTerm, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTermId(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTermId(this);
    }

    @Override public CAstId apply(ISubstitution.Immutable subst) {
        return new CAstId(subst.apply(term), subst.apply(idTerm), cause);
    }
    
    @Override
    public Optional<MConstraintResult> solve(IMState state, MConstraintContext params) throws Delay {
        final ITerm term = astTerm();
        final ITerm idTerm = idTerm();

        final IUnifier unifier = state.unifier();
        if(!(unifier.isGround(term))) {
            throw Delay.ofVars(unifier.getVars(term));
        }
        final CEqual eq;
        final Optional<Scope> maybeScope = AScope.matcher().match(term, unifier);
        if(maybeScope.isPresent()) {
            final AScope scope = maybeScope.get();
            eq = new CEqual(idTerm, scope);
            return Optional.of(MConstraintResult.ofConstraints(eq));
        } else {
            final Optional<TermIndex> maybeIndex = TermIndex.get(unifier.findTerm(term));
            if(maybeIndex.isPresent()) {
                final ITerm indexTerm = TermOrigin.copy(term, maybeIndex.get());
                eq = new CEqual(idTerm, indexTerm);
                return Optional.of(MConstraintResult.ofConstraints(eq));
            } else {
                return Optional.empty();
            }
        }
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("termId(");
        sb.append(termToString.format(term));
        sb.append(", ");
        sb.append(termToString.format(idTerm));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}