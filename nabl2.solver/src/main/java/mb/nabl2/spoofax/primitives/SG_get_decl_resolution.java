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
import mb.scopegraph.pepm16.esop15.IEsopNameResolution;
import mb.scopegraph.pepm16.path.IResolutionPath;
import mb.scopegraph.pepm16.terms.Label;
import mb.scopegraph.pepm16.terms.Occurrence;
import mb.scopegraph.pepm16.terms.Scope;
import mb.scopegraph.pepm16.terms.path.Paths;

public class SG_get_decl_resolution extends AnalysisPrimitive {

    public SG_get_decl_resolution() {
        super(SG_get_decl_resolution.class.getSimpleName(), 0);
    }

    @Override public Optional<? extends ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return Occurrence.matcher().match(term, solution.unifier()).<ITerm>flatMap(decl -> {
            IEsopNameResolution<Scope, Label, Occurrence> nameResolution = solution.nameResolution();
            List<ITerm> entries = new ArrayList<>();
            for(Occurrence ref : solution.scopeGraph().getAllRefs()) {
                try {
                    Collection<IResolutionPath<Scope, Label, Occurrence>> paths =
                            nameResolution.resolve(ref, new ThreadCancel(), new NullProgress());
                    paths.stream().filter(path -> path.getDeclaration().equals(decl))
                            .map(p -> B.newTuple(ref, Paths.toTerm(p))).forEach(entries::add);
                } catch(CriticalEdgeException | StuckException | InterruptedException e) {
                    // ignore
                }
            }
            return Optional.of(B.newList(entries));
        });
    }

}