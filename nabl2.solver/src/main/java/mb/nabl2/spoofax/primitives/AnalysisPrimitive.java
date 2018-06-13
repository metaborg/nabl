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
import mb.nabl2.spoofax.analysis.IScopeGraphContext;
import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
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
        return call(env, env.current(), termArgs, env.getFactory()).map(t -> {
            env.setCurrent(t);
            return true;
        }).orElse(false);
    }

    private Optional<? extends IStrategoTerm> call(IContext env, IStrategoTerm sterm, List<IStrategoTerm> sterms,
            ITermFactory factory) throws InterpreterException {
        if(sterms.size() < 1) {
            throw new IllegalArgumentException("Expected as first term argument: analysis");
        }
        final IStrategoTerm analysisTerm = sterms.get(0);
        final List<IStrategoTerm> argumentTerms = sterms.stream().skip(1).collect(Collectors.toList());
        final IScopeGraphUnit analysis;
        final Optional<IScopeGraphUnit> maybeAnalysis = StrategoBlob.match(analysisTerm, IScopeGraphUnit.class);
        if(maybeAnalysis.isPresent()) {
            analysis = maybeAnalysis.get();
        } else if(PrimitiveUtil.isAnalysisToken(analysisTerm)) {
            final IScopeGraphContext<?> context = PrimitiveUtil.scopeGraphContext(env);
            final String analysisResource = PrimitiveUtil.getAnalysisToken(analysisTerm);
            analysis = context.unit(analysisResource);
        } else {
            throw new IllegalArgumentException("Not a valid analysis term.");
        }
        if(analysis.solution().isPresent()) {
            return call(analysis, sterm, argumentTerms, factory);
        } else {
            return Optional.empty();
        }
    }

    protected Optional<? extends IStrategoTerm> call(IScopeGraphUnit unit, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        if(sterms.size() != tvars) {
            throw new InterpreterException("Expected " + tvars + " term arguments, but got " + sterms.size());
        }
        final StrategoTerms strategoTerms = new StrategoTerms(factory);
        final ITerm term = ConstraintTerms.specialize(strategoTerms.fromStratego(sterm));
        final List<ITerm> terms = sterms.stream().map(strategoTerms::fromStratego).map(ConstraintTerms::specialize)
                .collect(Collectors.toList());
        final Optional<? extends ITerm> result = call(unit, term, terms);
        return result.map(ConstraintTerms::explicate).map(strategoTerms::toStratego);
    }

    /** Override to get full analysis */
    protected Optional<? extends ITerm> call(IScopeGraphUnit unit, ITerm term,
            List<ITerm> terms) throws InterpreterException {
        if(unit.solution().isPresent()) {
            return call(unit.solution().get(), term, terms);
        } else {
            return Optional.empty();
        }
    }

    /** Override to get constraint solution */
    @SuppressWarnings("unused") protected Optional<? extends ITerm> call(ISolution solution, ITerm term,
            List<ITerm> terms) throws InterpreterException {
        throw new IllegalStateException("Method must be implemented by subclass.");
    }

}