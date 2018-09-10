package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.solver.ISolution;
import mb.nabl2.spoofax.analysis.IResult;
import mb.nabl2.terms.ITerm;

public abstract class AnalysisPrimitive extends NaBL2Primitive {

    final protected int tvars;

    public AnalysisPrimitive(String name) {
        this(name, 0);
    }

    public AnalysisPrimitive(String name, int tvars) {
        super(name, tvars + 1);
        this.tvars = tvars;
    }

    @Override protected Optional<? extends ITerm> call(ITerm term, List<ITerm> terms) throws InterpreterException {
        if(terms.size() < 1) {
            throw new IllegalArgumentException("Expected as first term argument: analysis");
        }
        final ITerm analysisTerm = terms.get(0);
        final List<ITerm> otherTerms = terms.stream().skip(1).collect(Collectors.toList());
        final IResult result = M.blobValue(IResult.class).match(analysisTerm)
                .orElseThrow(() -> new IllegalArgumentException("Not a valid analysis term."));
        return call(result, term, otherTerms);
    }

    protected Optional<? extends ITerm> call(IResult result, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return call(result.solution(), term, terms);
    }

    @SuppressWarnings("unused") protected Optional<? extends ITerm> call(ISolution solution, ITerm term,
            List<ITerm> terms) throws InterpreterException {
        return Optional.empty();
    }

}