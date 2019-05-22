package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableList;

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
            for(Occurrence ref : refs) {
                solution.nameResolution().resolve(ref).map(Paths::resolutionPathsToDecls).ifPresent(decls -> {
                    decls.stream().forEach(decl -> {
                        entriesBuilder.add(B.newTuple(ref, decl.getName()));
                    });
                });
            }
            final List<ITerm> entries = entriesBuilder.build();
            if(entries.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(B.newList(entries));
        });
    }

}