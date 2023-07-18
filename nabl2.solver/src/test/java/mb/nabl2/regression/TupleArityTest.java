package mb.nabl2.regression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.TermFactory;
import org.spoofax.terms.io.TAFTermReader;

import mb.nabl2.config.NaBL2DebugConfig;
import mb.nabl2.constraints.Constraints;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.base.IBaseConstraint;
import mb.nabl2.solver.Fresh;
import mb.nabl2.solver.ISolution;
import mb.nabl2.solver.SolverConfig;
import mb.nabl2.solver.exceptions.SolverException;
import mb.nabl2.solver.solvers.BaseSolution;
import mb.nabl2.solver.solvers.CallExternal;
import mb.nabl2.solver.solvers.GraphSolution;
import mb.nabl2.solver.solvers.SingleFileSolver;
import mb.nabl2.stratego.ConstraintTerms;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.nabl2.terms.unification.Unifiers;
import mb.scopegraph.pepm16.terms.ResolutionParameters;

@RunWith(Parameterized.class)
public class TupleArityTest {

    private static final int COUNT = 100000;
    private static final Random rnd = new Random(34856);


    private static final ITermFactory TF = new TermFactory();
    private static final StrategoTerms ST = new StrategoTerms(TF);
    private static final TAFTermReader reader = new TAFTermReader(TF);


    @Parameterized.Parameters public static Object[][] data() throws IOException, InterpreterException {
        final IStrategoTerm constraintSTerm =
            reader.read(TupleArityTest.class.getResourceAsStream("test.constraints.aterm"));
        final ITerm constraintTerm = ConstraintTerms.specialize(ST.fromStratego(constraintSTerm.getSubterm(0)));
        final IConstraint constraint = Constraints.matchConstraintOrList().match(constraintTerm)
            .orElseThrow(() -> new InterpreterException("Current term is not a constraint."));
        final List<IConstraint> constraints = disjoin(constraint);

        final Object[][] result = new Object[COUNT][1];
        for(int i = 0; i < COUNT; i++) {
            Collections.shuffle(constraints, rnd);
            result[i][0] = new ArrayList<>(constraints);
        }
        return result;
    }

    public TupleArityTest(List<IConstraint> permutation) {
        this.permutation = permutation;
    }

    private final List<IConstraint> permutation;

    @Test public void testTupleArityConstraintSet()
        throws IOException, InterpreterException, SolverException, InterruptedException {

        final SingleFileSolver solver = new SingleFileSolver(NaBL2DebugConfig.NONE, CallExternal.never());
        final Fresh.Transient fresh = Fresh.Transient.of();

        final SolverConfig config =
            SolverConfig.of(ResolutionParameters.getDefault(), Collections.emptyMap(), Collections.emptyMap());

        final GraphSolution gs = solver.solveGraph(BaseSolution.of(config, permutation, Unifiers.Immutable.of()),
            fresh::fresh, new NullCancel(), new NullProgress());

        final ISolution solution = solver.solve(gs, fresh::fresh, new NullCancel(), new NullProgress());

        Assert.assertFalse("Missing errors", solution.messagesAndUnsolvedErrors().getErrors().isEmpty());
    }

    public static List<IConstraint> disjoin(IConstraint constraint) {
        final ArrayList<IConstraint> constraints = new ArrayList<>();
        disjoin(constraint, constraints);
        return constraints;
    }

    private static boolean disjoin(IConstraint constraint, ArrayList<IConstraint> constraints) {
        // @formatter:off
		return constraint.match(IConstraint.Cases.of(
				c -> constraints.add(c),
				c -> c.match(IBaseConstraint.Cases.of(
						bc -> constraints.add(bc),
						bc -> constraints.add(bc),
						bc -> disjoin(bc.getLeft(), constraints) && disjoin(bc.getRight(), constraints),
						bc -> constraints.add(bc),
						bc -> constraints.add(bc))),
				c -> constraints.add(c),
				c -> constraints.add(c),
				c -> constraints.add(c),
				c -> constraints.add(c),
				c -> constraints.add(c),
				c -> constraints.add(c)
		));
		// @formatter:on
    }

}
