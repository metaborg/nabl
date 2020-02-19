package mb.statix.solver.persistent;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Test;
import org.metaborg.util.iterators.Iterables2;

import mb.nabl2.terms.ITermVar;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.spec.Spec;

public class SolverTest {

    @Test public void testExistentialCaptureOneLevel() throws InterruptedException {
        // this test uses knowledge of how the solver generates fresh variables
        final Spec spec = Spec.of();
        final ITermVar v1 = B.newVar("", "x");
        final ITermVar v2 = B.newVar("", "x1");
        final IConstraint constraint = new CExists(Arrays.asList(v1),
                new CExists(Arrays.asList(v2), new CInequal(Iterables2.empty(), v1, v2)));
        final SolverResult solveResult = Solver.solve(spec, State.of(spec), constraint, new NullDebugContext());

        // verify that the solver still names fresh variables as we assume
        assertEquals("Solver internals changed, test invalid.", v2, solveResult.existentials().get(v1));

        // test that capture did not happen
        assertFalse("Variable was captured.", solveResult.hasErrors());
    }

    @Test public void testExistentialCaptureTwoLevel() throws InterruptedException {
        // this test uses knowledge of how the solver generates fresh variables
        final Spec spec = Spec.of();
        final ITermVar v1 = B.newVar("", "x");
        final ITermVar v2 = B.newVar("", "y");
        final ITermVar v3 = B.newVar("", "x1");
        final IConstraint constraint = new CExists(Arrays.asList(v1),
                new CExists(Arrays.asList(v2),
                        new CExists(Arrays.asList(v3),
                                Constraints.conjoin(Arrays.asList(new CInequal(Iterables2.empty(), v1, v2),
                                        new CInequal(Iterables2.empty(), v2, v3),
                                        new CInequal(Iterables2.empty(), v3, v1))))));
        final SolverResult solveResult = Solver.solve(spec, State.of(spec), constraint, new NullDebugContext());

        // verify that the solver still names fresh variables as we assume
        assertEquals("Solver internals changed, test invalid.", v3, solveResult.existentials().get(v1));

        // test that capture did not happen
        assertFalse("Variable was captured.", solveResult.hasErrors());
    }

    @Test public void testExistentialCaptureThreeLevel() throws InterruptedException {
        // this test uses knowledge of how the solver generates fresh variables
        final Spec spec = Spec.of();
        final ITermVar v1 = B.newVar("", "x");
        final ITermVar v2 = B.newVar("", "x1");
        final ITermVar v3 = B.newVar("", "x0");
        final IConstraint constraint = new CExists(Arrays.asList(v1),
                new CExists(Arrays.asList(v2),
                        new CExists(Arrays.asList(v3),
                                Constraints.conjoin(Arrays.asList(new CInequal(Iterables2.empty(), v1, v2),
                                        new CInequal(Iterables2.empty(), v2, v3),
                                        new CInequal(Iterables2.empty(), v3, v1))))));
        final SolverResult solveResult = Solver.solve(spec, State.of(spec), constraint, new NullDebugContext());

        // verify that the solver still names fresh variables as we assume
        assertEquals("Solver internals changed, test invalid.", v2, solveResult.existentials().get(v1));

        // test that capture did not happen
        assertFalse("Variable was captured.", solveResult.hasErrors());
    }

}
