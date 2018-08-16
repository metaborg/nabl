package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.scopegraph.esop.IEsopNameResolution;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;

public class SG_get_decl_resolution extends AnalysisPrimitive {

    public SG_get_decl_resolution() {
        super(SG_get_decl_resolution.class.getSimpleName(), 0);
    }

    @Override public Optional<? extends ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return Occurrence.matcher().match(term, solution.unifier()).<ITerm>flatMap(decl -> {
            IEsopNameResolution<Scope, Label, Occurrence> nameResolution = solution.nameResolution();
            nameResolution.resolveAll();
            Stream<ITerm> resolutions = nameResolution.resolutionEntries().stream()
                    .flatMap(entry -> {
                        Occurrence from = entry.getKey();
                        return entry.getValue().stream()
                                .filter(path -> path.getDeclaration().equals(decl))
                                .map(p -> B.newTuple(from, Paths.toTerm(p)));
                    });
            return Optional.of(B.newList(() -> resolutions.iterator()));
        });
    }

}