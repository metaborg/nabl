package mb.statix.solver;

import java.util.Optional;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.constraint.CEqual;
import mb.statix.solver.constraint.CFalse;
import mb.statix.solver.constraint.CInequal;
import mb.statix.solver.constraint.CNew;
import mb.statix.solver.constraint.CPathDst;
import mb.statix.solver.constraint.CPathLabels;
import mb.statix.solver.constraint.CPathLt;
import mb.statix.solver.constraint.CPathMatch;
import mb.statix.solver.constraint.CPathScopes;
import mb.statix.solver.constraint.CPathSrc;
import mb.statix.solver.constraint.CResolveQuery;
import mb.statix.solver.constraint.CTellEdge;
import mb.statix.solver.constraint.CTellRel;
import mb.statix.solver.constraint.CTermId;
import mb.statix.solver.constraint.CTrue;
import mb.statix.solver.constraint.CUser;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;

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

    /**
     * Converts the given constraints to a comma separated string, using the given TermFormatter to
     * format the terms in each constraint.
     * 
     * @param constraints
     *      the constraints
     * @param termToString
     *      the term formatter
     * 
     * @return
     *      the string
     */
    static String toString(Iterable<? extends IConstraint> constraints, TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(IConstraint constraint : constraints) {
            if(!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(constraint.toString(termToString));
        }
        return sb.toString();
    }

    interface Cases<R> extends Function1<IConstraint, R> {

        R caseEqual(CEqual c);

        R caseFalse(CFalse c);

        R caseInequal(CInequal c);

        R caseNew(CNew c);

        R casePathDst(CPathDst c);

        R casePathLabels(CPathLabels c);

        R casePathLt(CPathLt c);

        R casePathMatch(CPathMatch c);

        R casePathScopes(CPathScopes c);

        R casePathSrc(CPathSrc c);

        R caseResolveQuery(CResolveQuery c);

        R caseTellEdge(CTellEdge c);

        R caseTellRel(CTellRel c);

        R caseTermId(CTermId c);

        R caseTrue(CTrue c);

        R caseUser(CUser c);

        @Override default R apply(IConstraint c) {
            return c.match(this);
        }

    }

    interface CheckedCases<R, E extends Throwable> extends CheckedFunction1<IConstraint, R, E> {

        R caseEqual(CEqual c) throws E;

        R caseFalse(CFalse c) throws E;

        R caseInequal(CInequal c) throws E;

        R caseNew(CNew c) throws E;

        R casePathDst(CPathDst c) throws E;

        R casePathLabels(CPathLabels c) throws E;

        R casePathLt(CPathLt c) throws E;

        R casePathMatch(CPathMatch c) throws E;

        R casePathScopes(CPathScopes c) throws E;

        R casePathSrc(CPathSrc c) throws E;

        R caseResolveQuery(CResolveQuery c) throws E;

        R caseTellEdge(CTellEdge c) throws E;

        R caseTellRel(CTellRel c) throws E;

        R caseTermId(CTermId c) throws E;

        R caseTrue(CTrue c) throws E;

        R caseUser(CUser c) throws E;

        @Override default R apply(IConstraint c) throws E {
            return c.matchOrThrow(this);
        }

    }

}