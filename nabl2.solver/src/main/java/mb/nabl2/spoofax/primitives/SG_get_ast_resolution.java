package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.task.NullProgress;
import org.metaborg.util.task.ThreadCancel;
import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.scopegraph.pepm16.CriticalEdgeException;
import mb.scopegraph.pepm16.StuckException;
import mb.scopegraph.pepm16.terms.Occurrence;
import mb.scopegraph.pepm16.terms.OccurrenceIndex;
import mb.scopegraph.pepm16.terms.path.Paths;

public class SG_get_ast_resolution extends AnalysisPrimitive {

    public SG_get_ast_resolution() {
        super(SG_get_ast_resolution.class.getSimpleName(), 0);
    }

    @Override public Optional<? extends ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return TermIndex.get(term).flatMap(index -> {
            final Collection<Occurrence> refs = solution.astRefs().get(OccurrenceIndex.of(index));
            final ImList.Transient<ITerm> entriesBuilder = ImList.Transient.of();
            try {
                for(Occurrence ref : refs) {
                    try {
                        final List<Occurrence> decls = Paths.resolutionPathsToDecls(
                                solution.nameResolution().resolve(ref, new ThreadCancel(), new NullProgress()));
                        decls.stream().forEach(decl -> {
                            entriesBuilder.add(B.newTuple(ref, decl.getName()));
                        });
                    } catch(CriticalEdgeException | StuckException e) {
                        // ignore
                    }
                }
            } catch(InterruptedException e) {
                return Optional.empty();
            }
            final List<ITerm> entries = entriesBuilder.freeze();
            if(entries.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(B.newList(entries));
        });
    }

}