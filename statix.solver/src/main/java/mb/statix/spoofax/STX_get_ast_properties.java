package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;


import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.statix.solver.ITermProperty;
import mb.statix.solver.persistent.SolverResult;

public class STX_get_ast_properties extends StatixPropertyPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_get_ast_properties() {
        super(STX_get_ast_properties.class.getSimpleName(), 1);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final SolverResult<?> analysis = M.blobValue(SolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));
        final Optional<TermIndex> maybeIndex = TermIndex.get(term);
        if(maybeIndex.isPresent()) {
            final TermIndex index = maybeIndex.get();
            final ImList.Mutable<ITerm> props = ImList.Mutable.of();
            for(Map.Entry<Tuple2<TermIndex, ITerm>, ITermProperty> prop : analysis.state().termProperties()
                    .entrySet()) {
                if(prop.getKey()._1().equals(index)) {
                    final ITerm propName = prop.getKey()._2();
                    final ITerm propValue = instantiateValue(prop.getValue(), analysis);
                    final ITerm multiplicity = explicate(prop.getValue().multiplicity());

                    props.add(B.newAppl(STX_PROP_OP, propName, propValue, multiplicity));
                }
            }

            return Optional.of(B.newList(props.freeze()));
        } else {
            return Optional.empty();
        }
    }

}
