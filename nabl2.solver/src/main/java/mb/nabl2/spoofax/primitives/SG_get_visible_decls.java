package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.task.NullProgress;
import org.metaborg.util.task.ThreadCancel;
import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.scopegraph.CriticalEdgeException;
import mb.nabl2.scopegraph.StuckException;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;

public class SG_get_visible_decls extends AnalysisPrimitive {

    public SG_get_visible_decls() {
        super(SG_get_visible_decls.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return Scope.matcher().match(term, solution.unifier()).<ITerm>flatMap(scope -> {
            try {
                return Optional.of(B.newList(solution.nameResolution().visible(scope, new ThreadCancel(), new NullProgress())));
            } catch(CriticalEdgeException | StuckException | InterruptedException e) {
                return Optional.empty();
            }
        });
    }

}