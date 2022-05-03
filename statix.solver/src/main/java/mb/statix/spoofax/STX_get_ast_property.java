package mb.statix.spoofax;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.inject.Inject;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.statix.solver.ITermProperty;
import mb.statix.solver.persistent.SolverResult;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

public class STX_get_ast_property extends StatixPrimitive {

    @Inject public STX_get_ast_property() {
        super(STX_get_ast_property.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final SolverResult analysis = M.blobValue(SolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));
        final ITerm prop = terms.get(1);
        warnOnInvalidProp(prop);
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

    private void warnOnInvalidProp(ITerm prop) {
        if(prop instanceof IApplTerm) {
            final IApplTerm applTerm = (IApplTerm) prop;
            final String termOp = applTerm.getOp();
            switch(termOp) {
                case "Type":
                    if(applTerm.getArity() == 0) {
                        return;
                    }
                    break;
                case "Ref":
                    if(applTerm.getArity() == 0) {
                        return;
                    }
                    break;
                case "Prop":
                    if(applTerm.getArity() == 1 && applTerm.getArgs()
                        .get(0) instanceof IStringTerm) {
                        return;
                    }
                    break;
            }
        }
        logger.warn("Expected Type(), Ref() or Prop(\"<name>\") as property, but got {}.", prop);
    }

}