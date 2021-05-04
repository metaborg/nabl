package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.scopegraph.pepm16.terms.Occurrence;
import mb.scopegraph.pepm16.terms.OccurrenceIndex;

public class SG_get_ast_decls extends AnalysisPrimitive {

    public SG_get_ast_decls() {
        super(SG_get_ast_decls.class.getSimpleName());
    }

    @Override public Optional<ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return TermIndex.get(term).flatMap(index -> {
            final Collection<Occurrence> decls = solution.astDecls().get(OccurrenceIndex.of(index));
            return Optional.of(B.newList(decls));
        });
    }

}