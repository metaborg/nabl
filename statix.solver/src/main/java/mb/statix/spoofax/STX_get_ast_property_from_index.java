package mb.statix.spoofax;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.stratego.TermIndex;
import mb.statix.solver.ITermProperty;
import mb.statix.solver.persistent.SolverResult;
import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import java.util.List;
import java.util.Optional;

import static mb.nabl2.terms.matching.TermMatch.M;

/**
 * Gets an AST property for some term with the given term index.
 */
public final class STX_get_ast_property_from_index extends StatixPropertyPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_get_ast_property_from_index() {
        super(STX_get_ast_property_from_index.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms) throws InterpreterException {
        final SolverResult analysis = M.blobValue(SolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));
        final ITerm prop = terms.get(1);
        final Optional<TermIndex> indexOpt = TermIndex.matcher().match(term);
        if (indexOpt.isPresent()) {
            return call(env, indexOpt.get(), analysis, prop);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<? extends ITerm> call(IContext env, TermIndex index, SolverResult analysis, ITerm prop) throws InterpreterException {
        warnOnInvalidProp(prop);
        final Tuple2<TermIndex, ITerm> key = Tuple2.of(index, prop);
        if(!analysis.state().termProperties().containsKey(key)) {
            return Optional.empty();
        }
        final ITermProperty property = analysis.state().termProperties().get(key);
        return Optional.of(instantiateValue(property, analysis));
    }

    private static void warnOnInvalidProp(ITerm prop) {
        // @formatter:off
        IMatcher<ITerm> propMatcher = M.cases(
            M.appl0("Type"),
            M.appl0("Ref"),
            M.appl1("Prop", M.string())
        );
        // @formatter:on
        if(!propMatcher.match(prop).isPresent()) {
            logger.warn("Expected Type(), Ref() or Prop(\"<name>\") as property, but got {}.", prop);
        }
    }

}
