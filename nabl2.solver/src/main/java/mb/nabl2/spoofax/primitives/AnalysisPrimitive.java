package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.util.NotImplementedException;

import mb.nabl2.spoofax.analysis.IScopeGraphContext;
import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
import mb.nabl2.stratego.ConstraintTerms;
import mb.nabl2.stratego.StrategoBlob;
import mb.nabl2.stratego.StrategoTermIndices;
import mb.nabl2.stratego.StrategoTerms;
import mb.nabl2.terms.ITerm;

public abstract class AnalysisPrimitive extends ScopeGraphContextPrimitive {

    public AnalysisPrimitive(String name) {
        this(name, 0);
    }

    public AnalysisPrimitive(String name, int tvars) {
        super(name, 0, tvars + 1);
    }

    @Override public Optional<? extends IStrategoTerm> call(IScopeGraphContext<?> context, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        if(sterms.size() < 1) {
            throw new IllegalArgumentException("Expected as first term argument: analysis");
        }
        final IStrategoTerm analysisTerm = sterms.get(0);
        final IScopeGraphUnit analysis;
        final Optional<IScopeGraphUnit> maybeAnalysis = StrategoBlob.match(analysisTerm, IScopeGraphUnit.class);
        if(maybeAnalysis.isPresent()) {
            analysis = maybeAnalysis.get();
        } else if(Tools.isTermAppl(analysisTerm)
                && Tools.hasConstructor((IStrategoAppl) analysisTerm, "AnalysisToken", 0)) {
            // TODO Remove legacy case after bootstrapping
            analysis = StrategoTermIndices.get(analysisTerm)
                    .map(idx -> context.unit(idx.getResource()))
                    .orElseThrow(() -> new IllegalArgumentException("Not a valid analysis term."));
        } else {
            throw new IllegalArgumentException("Not a valid analysis term.");
        }
        return call(analysis, sterm, sterms, factory);
    }

    public Optional<? extends IStrategoTerm> call(IScopeGraphUnit unit, IStrategoTerm sterm, List<IStrategoTerm> sterms,
            ITermFactory factory) throws InterpreterException {
        final StrategoTerms strategoTerms = new StrategoTerms(factory);
        final List<ITerm> terms = sterms.stream().skip(1).map(strategoTerms::fromStratego)
                .map(ConstraintTerms::specialize).collect(Collectors.toList());
        final ITerm term = ConstraintTerms.specialize(strategoTerms.fromStratego(sterm));
        Optional<? extends ITerm> result = call(unit, term, terms);
        return result.map(ConstraintTerms::explicate).map(strategoTerms::toStratego);
    }

    @SuppressWarnings("unused") public Optional<? extends ITerm> call(IScopeGraphUnit unit, ITerm term,
            List<ITerm> terms) throws InterpreterException {
        throw new NotImplementedException("Subclasses should override call method.");
    }

}