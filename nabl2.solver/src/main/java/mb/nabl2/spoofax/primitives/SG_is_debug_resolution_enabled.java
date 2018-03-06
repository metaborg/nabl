package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import mb.nabl2.spoofax.analysis.IScopeGraphContext;

public class SG_is_debug_resolution_enabled extends ScopeGraphContextPrimitive {

    public SG_is_debug_resolution_enabled() {
        super(SG_is_debug_resolution_enabled.class.getSimpleName(), 0, 0);
    }

    @Override public Optional<? extends IStrategoTerm> call(IScopeGraphContext<?> context, IStrategoTerm term,
            List<IStrategoTerm> termVars, ITermFactory factory) throws InterpreterException {
        return context.config().debug().resolution() ? Optional.of(term) : Optional.empty();
    }

}