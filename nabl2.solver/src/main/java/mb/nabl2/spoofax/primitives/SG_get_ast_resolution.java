package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableList;

import mb.nabl2.scopegraph.CriticalEdgeException;
import mb.nabl2.scopegraph.StuckException;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.OccurrenceIndex;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;

public class SG_get_ast_resolution extends AnalysisPrimitive {

    public SG_get_ast_resolution() {
        super(SG_get_ast_resolution.class.getSimpleName(), 0);
    }

    @Override public Optional<? extends ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return TermIndex.get(term).flatMap(index -> {
            final Collection<Occurrence> refs = solution.astRefs().get(OccurrenceIndex.of(index));
            final ImmutableList.Builder<ITerm> entriesBuilder = ImmutableList.builder();
            try {
                for(Occurrence ref : refs) {
                    try {
                        final List<Occurrence> decls =
                                Paths.resolutionPathsToDecls(solution.nameResolution().resolve(ref));
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
            final List<ITerm> entries = entriesBuilder.build();
            if(entries.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(B.newList(entries));
        });
    }

}