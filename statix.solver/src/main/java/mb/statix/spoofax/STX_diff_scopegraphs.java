package mb.statix.spoofax;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.terms.ITerm;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiffer;
import mb.statix.scopegraph.Scope;
import mb.statix.scopegraph.StatixDifferOps;
import mb.statix.solver.IState;
import mb.statix.solver.persistent.SolverResult;

public class STX_diff_scopegraphs extends StatixPrimitive {

    public STX_diff_scopegraphs() {
        super(STX_diff_scopegraphs.class.getSimpleName(), 1);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        final Scope s0 =
                Scope.matcher().match(terms.get(0)).orElseThrow(() -> new java.lang.IllegalArgumentException());

        final Tuple2<IState.Immutable, IState.Immutable> states =
                M.tuple2(M.blobValue(SolverResult.class), M.blobValue(SolverResult.class), (t, current, previous) -> {
                    return Tuple2.of(current.state(), previous.state());
                }).match(term).orElseThrow(() -> new IllegalArgumentException("Expected solver results, got " + term));
        final IState.Immutable current = states._1();
        final IState.Immutable previous = states._2();

        final ScopeGraphDiff<Scope, ITerm, ITerm> diff = ScopeGraphDiffer.fullDiff(s0, s0, current.scopeGraph(),
                previous.scopeGraph(), new StatixDifferOps(current.unifier(), previous.unifier()));

        return Optional.of(StatixDifferOps.toTerm(diff, current.unifier(), previous.unifier()));
    }

}