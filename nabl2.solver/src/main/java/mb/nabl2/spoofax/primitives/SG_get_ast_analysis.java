package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import mb.nabl2.spoofax.analysis.IScopeGraphContext;
import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
import mb.nabl2.stratego.StrategoBlob;
import mb.nabl2.stratego.StrategoTermIndices;

public class SG_get_ast_analysis extends ScopeGraphContextPrimitive {

    public SG_get_ast_analysis() {
        super(SG_get_ast_analysis.class.getSimpleName(), 0);
    }

    @Override public Optional<? extends IStrategoTerm> call(IScopeGraphContext<?> context, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        return StrategoTermIndices.get(sterm).map(index -> {
            final IScopeGraphUnit unit = context.unit(index.getResource());
            return new StrategoBlob(unit);
        });
    }

}