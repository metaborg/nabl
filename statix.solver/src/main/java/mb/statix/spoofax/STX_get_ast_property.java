package mb.statix.spoofax;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;


import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.stratego.TermIndex;
import mb.statix.solver.ITermProperty;
import mb.statix.solver.persistent.SolverResult;

/**
 * Gets an AST property for a given term.
 */
public final class STX_get_ast_property extends StatixPropertyPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_get_ast_property() {
        super(STX_get_ast_property.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms) throws InterpreterException {
        final SolverResult<?> analysis = M.blobValue(SolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));
        final ITerm prop = terms.get(1);
        return call(env, term, analysis, prop);
    }

    public static Optional<? extends ITerm> call(IContext env, ITerm term, SolverResult analysis, ITerm prop) throws InterpreterException {
        final Optional<TermIndex> indexOpt = TermIndex.get(term);

        if(indexOpt.isPresent()) {
            return STX_get_ast_property_from_index.call(env, indexOpt.get(), analysis, prop);
        } else {
            return Optional.empty();
        }
    }

}
