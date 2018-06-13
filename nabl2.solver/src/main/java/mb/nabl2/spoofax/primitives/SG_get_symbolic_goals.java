package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;

public class SG_get_symbolic_goals extends AnalysisPrimitive {

    public SG_get_symbolic_goals() {
        super(SG_get_symbolic_goals.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return Optional.of(B.newList(solution.symbolic().getGoals().stream().map(solution.unifier()::findRecursive)
                .collect(Collectors.toSet())));
    }

}