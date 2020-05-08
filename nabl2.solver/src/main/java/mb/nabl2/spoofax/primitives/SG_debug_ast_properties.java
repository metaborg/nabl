package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.constraints.ast.AstProperties;
import mb.nabl2.solver.ISolution;
import mb.nabl2.spoofax.analysis.IResult;
import mb.nabl2.terms.ITerm;

public class SG_debug_ast_properties extends AnalysisPrimitive {

    public SG_debug_ast_properties() {
        super(SG_debug_ast_properties.class.getSimpleName());
    }

    @Override protected Optional<? extends ITerm> call(IResult result, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if(result.partial()) {
            return Optional.empty();
        }
        final ISolution sol = result.solution();
        return Optional.of(AstProperties.build(sol.astProperties(), sol.unifier()));
    }

}