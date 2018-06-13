package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.solver.ISolution;
import mb.nabl2.stratego.TermIndex;
import mb.nabl2.terms.ITerm;

public class SG_get_ast_property extends AnalysisPrimitive {

    public SG_get_ast_property() {
        super(SG_get_ast_property.class.getSimpleName(), 1);
    }

    @Override public Optional<? extends ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final ITerm key = terms.get(0);
        return TermIndex.get(term).<ITerm>flatMap(index -> {
            return solution.astProperties().getValue(index, key).map(solution.unifier()::findRecursive);
        });
    }

}