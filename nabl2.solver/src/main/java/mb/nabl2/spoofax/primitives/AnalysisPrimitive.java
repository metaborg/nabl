package mb.nabl2.spoofax.primitives;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import mb.nabl2.solver.ISolution;
import mb.nabl2.spoofax.analysis.FinalResult;
import mb.nabl2.stratego.ConstraintTerms;
import mb.nabl2.stratego.StrategoBlob;
import mb.nabl2.stratego.StrategoTerms;
import mb.nabl2.terms.ITerm;

public abstract class AnalysisPrimitive extends AbstractPrimitive {

    final protected int tvars;

    public AnalysisPrimitive(String name) {
        this(name, 0);
    }

    public AnalysisPrimitive(String name, int tvars) {
        super(name, 0, tvars + 1);
        this.tvars = tvars;
    }

    @Override public final boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars)
            throws InterpreterException {
        final List<IStrategoTerm> termArgs = Arrays.asList(tvars);
        return call(env.current(), termArgs, env.getFactory()).map(t -> {
            env.setCurrent(t);
            return true;
        }).orElse(false);
    }

    private Optional<? extends IStrategoTerm> call(IStrategoTerm sterm, List<IStrategoTerm> sterms,
            ITermFactory factory) throws InterpreterException {
        if(sterms.size() < 1) {
            throw new IllegalArgumentException("Expected as first term argument: analysis");
        }
        final IStrategoTerm analysisTerm = sterms.get(0);
        final List<IStrategoTerm> argumentTerms = sterms.stream().skip(1).collect(Collectors.toList());
        final FinalResult analysis = StrategoBlob.match(analysisTerm, FinalResult.class)
                .orElseThrow(() -> new IllegalArgumentException("Not a valid analysis term."));
        return call(analysis.solution(), sterm, argumentTerms, factory);
    }

    protected Optional<? extends IStrategoTerm> call(ISolution solution, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        if(sterms.size() != tvars) {
            throw new InterpreterException("Expected " + tvars + " term arguments, but got " + sterms.size());
        }
        final StrategoTerms strategoTerms = new StrategoTerms(factory);
        final ITerm term = ConstraintTerms.specialize(strategoTerms.fromStratego(sterm));
        final List<ITerm> terms = sterms.stream().map(strategoTerms::fromStratego).map(ConstraintTerms::specialize)
                .collect(Collectors.toList());
        final Optional<? extends ITerm> result = call(solution, term, terms);
        return result.map(ConstraintTerms::explicate).map(strategoTerms::toStratego);
    }

    protected abstract Optional<? extends ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException;

}