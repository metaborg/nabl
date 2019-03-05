package mb.statix.solver;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;
import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITermVar;
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

public interface IConstraint {

    /**
     * Solve constraint
     * 
     * @param state
     *            -- monotonic from one call to the next
     * @param params
     * @return true is reduced, false if delayed
     * @throws InterruptedException
     * @throws Delay
     */
    Optional<ConstraintResult> solve(State state, ConstraintContext params) throws InterruptedException, Delay;

    Optional<IConstraint> cause();

    IConstraint withCause(IConstraint cause);

    <R> R match(Cases<R> cases);

    IConstraint apply(ISubstitution.Immutable subst);

    String toString(TermFormatter termToString);

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

    @Value.Immutable
    static abstract class AConstraintResult {

        @Value.Parameter public abstract State state();

        @Value.Parameter public abstract List<IConstraint> constraints();

        @Value.Parameter public abstract List<ITermVar> vars();

        public static ConstraintResult of(State state) {
            return ConstraintResult.of(state, ImmutableList.of(), ImmutableList.of());
        }

        public static ConstraintResult ofConstraints(State state, IConstraint... constraints) {
            return ofConstraints(state, Arrays.asList(constraints));
        }

        public static ConstraintResult ofConstraints(State state, Iterable<? extends IConstraint> constraints) {
            return ConstraintResult.of(state, ImmutableList.copyOf(constraints), ImmutableList.of());
        }

        public static ConstraintResult ofVars(State state, Iterable<? extends ITermVar> vars) {
            return ConstraintResult.of(state, ImmutableList.of(), ImmutableList.copyOf(vars));
        }

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

        default R apply(IConstraint c) {
            return c.match(this);
        }

    }

}