package mb.statix.spoofax;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.ISolverResult;

public class STX_get_ast_property extends StatixPrimitive {

    @Inject public STX_get_ast_property() {
        super(STX_get_ast_property.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final ISolverResult analysis = M.blobValue(ISolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));
        final ITerm property = terms.get(1);
        final Optional<TermIndex> maybeIndex = TermIndex.get(term);
        if(maybeIndex.isPresent()) {
            final TermIndex index = maybeIndex.get();
            final Tuple2<TermIndex, ITerm> key = ImmutableTuple2.of(index, property);
            final ITerm value = analysis.state().termProperties().get(key);
            return Optional.ofNullable(value).map(analysis.state().unifier().unrestricted()::findRecursive);
        } else {
            return Optional.empty();
        }
    }

}