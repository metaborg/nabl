package mb.statix.solver;

import java.util.Optional;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CFalse;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CNew;
import mb.statix.constraints.CPathLt;
import mb.statix.constraints.CPathMatch;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CTellEdge;
import mb.statix.constraints.CTellRel;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CUser;
import mb.statix.modular.solver.MConstraintContext;
import mb.statix.modular.solver.MConstraintResult;
import mb.statix.modular.solver.state.IMState;

/**
 * Interface to represent a constraint.
 */
public interface IConstraint {

    /**
     * @return
     *      the constraint that caused this constraint to be added
     */
    Optional<IConstraint> cause();

    /**
     * Creates a copy of the current constraint with the given cause set as cause.
     * 
     * @param cause
     *      the cause
     * 
     * @return
     *      the copied constraint
     */
    IConstraint withCause(IConstraint cause);

    /**
     * Solves this constraint with mutable state.
     * 
     * @param state
     *      mutable state
     * @param params
     *      the context containing info about completeness, rigid and closed as well as debug
     *      
     * @return
     *      true is reduced, false if delayed
     * 
     * @throws InterruptedException
     *      Optional exception that is thrown when solving this constraint is interrupted.
     *      
     * @throws Delay
     *      If this constraint cannot be solved in the current state with the given context.
     *      The exception contains the information about what information is required to solve.
     */
    Optional<MConstraintResult> solve(IMState state, MConstraintContext params) throws InterruptedException, Delay;

    <R> R match(Cases<R> cases);

    <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E;

    /**
     * Applies the given substitution to this constraint.
     * 
     * @param subst
     *      the substitution
     * 
     * @return
     *      a copy of this constraint with the given substitution applied
     */
    IConstraint apply(ISubstitution.Immutable subst);

    /**
     * Converts this constraint to a string, where terms are formatted using the given term
     * formatter.
     * 
     * @param termToString
     *      the term formatter for formatting terms in this constraint
     * 
     * @return
     *      the string
     */
    String toString(TermFormatter termToString);

    interface Cases<R> extends Function1<IConstraint, R> {

        R caseConj(CConj c);

        R caseEqual(CEqual c);

        R caseExists(CExists c);

        R caseFalse(CFalse c);

        R caseInequal(CInequal c);

        R caseNew(CNew c);

        R casePathLt(CPathLt c);

        R casePathMatch(CPathMatch c);

        R caseResolveQuery(CResolveQuery c);

        R caseTellEdge(CTellEdge c);

        R caseTellRel(CTellRel c);

        R caseTermId(CAstId c);

        R caseTermProperty(CAstProperty c);

        R caseTrue(CTrue c);

        R caseUser(CUser c);

        @Override default R apply(IConstraint c) {
            return c.match(this);
        }

    }

    interface CheckedCases<R, E extends Throwable> extends CheckedFunction1<IConstraint, R, E> {

        R caseConj(CConj c) throws E;

        R caseEqual(CEqual c) throws E;

        R caseExists(CExists c) throws E;

        R caseFalse(CFalse c) throws E;

        R caseInequal(CInequal c) throws E;

        R caseNew(CNew c) throws E;

        R casePathLt(CPathLt c) throws E;

        R casePathMatch(CPathMatch c) throws E;

        R caseResolveQuery(CResolveQuery c) throws E;

        R caseTellEdge(CTellEdge c) throws E;

        R caseTellRel(CTellRel c) throws E;

        R caseTermId(CAstId c) throws E;

        R caseTermProperty(CAstProperty c) throws E;

        R caseTrue(CTrue c) throws E;

        R caseUser(CUser c) throws E;

        @Override default R apply(IConstraint c) throws E {
            return c.matchOrThrow(this);
        }

    }

}