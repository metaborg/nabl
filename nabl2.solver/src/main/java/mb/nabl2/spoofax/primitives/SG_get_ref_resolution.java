package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.task.NullProgress;
import org.metaborg.util.task.ThreadCancel;
import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;
import mb.scopegraph.pepm16.CriticalEdgeException;
import mb.scopegraph.pepm16.StuckException;
import mb.scopegraph.pepm16.path.IResolutionPath;
import mb.scopegraph.pepm16.terms.Label;
import mb.scopegraph.pepm16.terms.Occurrence;
import mb.scopegraph.pepm16.terms.Scope;
import mb.scopegraph.pepm16.terms.path.Paths;

public class SG_get_ref_resolution extends AnalysisPrimitive {

    public SG_get_ref_resolution() {
        super(SG_get_ref_resolution.class.getSimpleName(), 0);
    }

    @Override public Optional<? extends ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final Optional<Occurrence> maybeRef = Occurrence.matcher().match(term, solution.unifier());
        return maybeRef.<ITerm>flatMap(ref -> {
            try {
                final Collection<IResolutionPath<Scope, Label, Occurrence>> paths =
                        solution.nameResolution().resolve(ref, new ThreadCancel(), new NullProgress());
                List<ITerm> pathTerms = new ArrayList<>(paths.size());
                for(IResolutionPath<Scope, Label, Occurrence> path : paths) {
                    pathTerms.add(B.newTuple(path.getDeclaration(), Paths.toTerm(path)));
                }
                ITerm result = B.newList(pathTerms);
                return Optional.of(result);
            } catch(CriticalEdgeException | StuckException | InterruptedException e) {
                return Optional.empty();
            }
        });
    }

}