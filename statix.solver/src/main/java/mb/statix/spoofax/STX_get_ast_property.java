package mb.statix.spoofax;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.statix.solver.persistent.SolverResult;

public class STX_get_ast_property extends StatixPrimitive {

    @Inject public STX_get_ast_property() {
        super(STX_get_ast_property.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final SolverResult analysis = M.blobValue(SolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));
        final ITerm property = terms.get(1);
        final Optional<TermIndex> maybeIndex = TermIndex.get(term);
        if(maybeIndex.isPresent()) {
            final TermIndex index = maybeIndex.get();
            return analysis.state().termProperties().get(index, property).stream().findFirst()
                    .map(analysis.state().unifier()::findRecursive);
        } else {
            return Optional.empty();
        }
    }

}