package mb.statix.spoofax;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.collection.ImList;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;


import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.u.PersistentUnifier;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.reference.ScopeGraph;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.persistent.SolverResult;

public class STX_get_scopegraph extends StatixPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_get_scopegraph() {
        super(STX_get_scopegraph.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        // @formatter:off
        final List<SolverResult> analyses = M.cases(
            M.blobValue(SolverResult.class).map(ImList.Immutable::of),
            M.listElems(M.blobValue(SolverResult.class))
        ).match(term).orElseThrow(() -> new InterpreterException("Expected solver result."));
        // @formatter:on

        final IScopeGraph.Transient<Scope, ITerm, ITerm> scopeGraph = ScopeGraph.Transient.of();
        final IUnifier.Transient unifier = PersistentUnifier.Immutable.of().melt();
        for(SolverResult<?> analysis : analyses) {
            scopeGraph.addAll(analysis.state().scopeGraph());
            try {
                unifier.unify(analysis.state().unifier());
            } catch(OccursException e) {
                throw new InterpreterException("Cannot combine unifiers.", e);
            }
        }

        final ITerm scopeGraphTerm = StatixTerms.toTerm(scopeGraph.freeze(), unifier.freeze());

        return Optional.of(scopeGraphTerm);
    }

}
