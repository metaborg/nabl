package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.statix.solver.ISolverResult;

public class STX_delays_as_errors extends StatixPrimitive {

    @Inject public STX_delays_as_errors() {
        super(STX_delays_as_errors.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        final ISolverResult result = M.blobValue(ISolverResult.class).match(term)
                .orElseThrow(() -> new InterpreterException("Expected solver result."));
        final ISolverResult newResult =
                result.withErrors(Sets.union(result.errors(), result.delays().keySet())).withDelays(ImmutableMap.of());
        return Optional.of(B.newBlob(newResult));
    }

}