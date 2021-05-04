package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.task.NullProgress;
import org.metaborg.util.task.ThreadCancel;
import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.solver.ISolution;
import mb.nabl2.spoofax.analysis.IResult;
import mb.nabl2.terms.ITerm;
import mb.scopegraph.pepm16.terms.NameResolutionTerms;

public class SG_debug_name_resolution extends AnalysisPrimitive {

    public SG_debug_name_resolution() {
        super(SG_debug_name_resolution.class.getSimpleName());
    }

    @Override protected Optional<? extends ITerm> call(IResult result, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if(result.partial()) {
            return Optional.empty();
        }
        final ISolution sol = result.solution();
        try {
            return Optional.of(NameResolutionTerms.build(sol.scopeGraph(), sol.nameResolution(), new ThreadCancel(), new NullProgress()));
//          return result.solution().filter(sol -> unit.isPrimary()).map(sol -> {
//              return TermSimplifier.focus(unit.resource(),
//                    NameResolutionTerms.build(sol.scopeGraph(), sol.nameResolution()));
//          });
        } catch(InterruptedException e) {
            return Optional.empty();
        }
    }

}