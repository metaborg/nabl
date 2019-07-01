package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.statix.taico.dot.DotPrinter;
import mb.statix.taico.solver.MSolverResult;

public class MSTX_scope_graph extends StatixPrimitive {

    @Inject public MSTX_scope_graph() {
        super(MSTX_scope_graph.class.getSimpleName(), 1);
    }

    @Override
    protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms) throws InterpreterException {
        final MSolverResult initial = M.blobValue(MSolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result, but was " + terms.get(0)))
                .reset();
        
        String file = M.stringValue().match(term).orElse(null);
        DotPrinter printer = new DotPrinter(initial, file);
        return Optional.of(B.newString(printer.printDot()));
    }
}