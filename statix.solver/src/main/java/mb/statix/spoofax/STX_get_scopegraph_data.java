package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;


import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.IState;
import mb.statix.solver.persistent.SolverResult;

public class STX_get_scopegraph_data extends StatixPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_get_scopegraph_data() {
        super(STX_get_scopegraph_data.class.getSimpleName(), 1);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final SolverResult<?> analysis = M.blobValue(SolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));
        final IState.Immutable state = analysis.state();
        // @formatter:off
        final ITerm data = M.cases(
            M.tuple2(Scope.matcher(), StatixTerms.label(), (t, s, r) -> {
                reportInvalidDataLabel(analysis, r);
                final IUniDisunifier.Immutable unifier = state.unifier();
                return B.newList(state.scopeGraph().getEdges(s, r).stream()
                        .map(state.scopeGraph()::getData)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(unifier::findRecursive)
                        .collect(Collectors.toList()));
            })
        ).match(term).orElseThrow(() -> new InterpreterException("Expected scope-label pair."));
        // @formatter:on
        return Optional.of(data);
    }

}
