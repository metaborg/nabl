package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;

import mb.nabl2.scopegraph.CriticalEdgeException;
import mb.nabl2.scopegraph.StuckException;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;

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
                        solution.nameResolution().resolve(ref);
                List<ITerm> pathTerms = Lists.newArrayListWithExpectedSize(paths.size());
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