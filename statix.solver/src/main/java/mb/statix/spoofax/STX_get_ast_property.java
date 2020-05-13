package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.ITermProperty;
import mb.statix.solver.persistent.SolverResult;

public class STX_get_ast_property extends StatixPrimitive {

    @Inject public STX_get_ast_property() {
        super(STX_get_ast_property.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final SolverResult analysis = M.blobValue(SolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));
        final ITerm prop = terms.get(1);
        final Optional<TermIndex> maybeIndex = TermIndex.get(term);
        if(maybeIndex.isPresent()) {
            final TermIndex index = maybeIndex.get();
            final Tuple2<TermIndex, ITerm> key = Tuple2.of(index, prop);
            if(!analysis.state().termProperties().containsKey(key)) {
                return Optional.empty();
            }
            final ITermProperty property = analysis.state().termProperties().get(key);
            final ITerm result;
            switch(property.multiplicity()) {
                case BAG: {
                    result = B.newList(Streams.stream(property.values()).map(analysis.state().unifier()::findRecursive)
                            .collect(ImmutableList.toImmutableList()));
                    break;
                }
                case SINGLETON: {
                    result = analysis.state().unifier().findRecursive(property.value());
                    break;
                }
                default:
                    throw new IllegalStateException("Unknown multiplicity " + property.multiplicity());
            }
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }

}