package mb.nabl2.spoofax.primitives;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.concurrent.IClosableLock;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import mb.nabl2.spoofax.analysis.IScopeGraphContext;
import mb.nabl2.stratego.ConstraintTerms;
import mb.nabl2.stratego.StrategoTerms;
import mb.nabl2.terms.ITerm;

public abstract class ScopeGraphContextPrimitive extends AbstractPrimitive {

    final protected int tvars;

    public ScopeGraphContextPrimitive(String name) {
        this(name, 0);
    }

    public ScopeGraphContextPrimitive(String name, int tvars) {
        super(name, 0, tvars);
        this.tvars = tvars;
    }

    @Override public final boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars)
            throws InterpreterException {
        final IScopeGraphContext<?> context = PrimitiveUtil.scopeGraphContext(env);
        final List<IStrategoTerm> termArgs = Arrays.asList(tvars);
        final Optional<? extends IStrategoTerm> result;
        try(IClosableLock lock = context.guard()) {
            result = call(context, env.current(), termArgs, env.getFactory());
        }
        return result.map(t -> {
            env.setCurrent(t);
            return true;
        }).orElse(false);
    }

    protected Optional<? extends IStrategoTerm> call(IScopeGraphContext<?> context, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        if(sterms.size() != tvars) {
            throw new InterpreterException("Expected " + tvars + " term arguments, but got " + sterms.size());
        }
        final StrategoTerms strategoTerms = new StrategoTerms(factory);
        final ITerm term = ConstraintTerms.specialize(strategoTerms.fromStratego(sterm));
        final List<ITerm> terms = sterms.stream().map(strategoTerms::fromStratego).map(ConstraintTerms::specialize)
                .collect(Collectors.toList());
        final Optional<? extends ITerm> result = call(context, term, terms);
        return result.map(ConstraintTerms::explicate).map(strategoTerms::toStratego);
    }

    @SuppressWarnings("unused") protected Optional<? extends ITerm> call(IScopeGraphContext<?> context, ITerm term,
            List<ITerm> terms) throws InterpreterException {
        throw new IllegalStateException("Method must be implemented by subclass.");
    }

}