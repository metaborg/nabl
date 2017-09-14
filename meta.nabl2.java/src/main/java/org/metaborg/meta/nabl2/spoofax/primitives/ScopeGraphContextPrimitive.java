package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.ConstraintTerms;
import org.metaborg.meta.nabl2.stratego.StrategoTerms;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

public abstract class ScopeGraphContextPrimitive extends AbstractPrimitive {

    private static ILogger logger = LoggerUtils.logger(ScopeGraphContextPrimitive.class);

    public ScopeGraphContextPrimitive(String name, int svars, int tvars) {
        super(name, svars, tvars);
    }

    @Override public final boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars)
            throws InterpreterException {
        final Object contextObj = env.contextObject();
        if(contextObj == null) {
            logger.warn("Context is null.");
            return false;
        }
        if(!(contextObj instanceof IScopeGraphContext)) {
            throw new InterpreterException("Context does not implement IScopeGraphContext");
        }
        final IScopeGraphContext<?> context = (IScopeGraphContext<?>) env.contextObject();
        List<IStrategoTerm> termArgs = Arrays.asList(tvars);
        Optional<? extends IStrategoTerm> result;
        try(IClosableLock lock = context.guard()) {
            result = call(context, env.current(), termArgs, env.getFactory());
        }
        return result.map(t -> {
            env.setCurrent(t);
            return true;
        }).orElse(false);
    }

    public Optional<? extends IStrategoTerm> call(IScopeGraphContext<?> context, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        StrategoTerms strategoTerms = new StrategoTerms(factory);
        ITerm term = ConstraintTerms.specialize(strategoTerms.fromStratego(sterm));
        List<ITerm> terms = sterms.stream().map(strategoTerms::fromStratego).map(ConstraintTerms::specialize)
                .collect(Collectors.toList());
        Optional<? extends ITerm> result = call(context, term, terms);
        return result.map(ConstraintTerms::explicate).map(strategoTerms::toStratego);
    }

    @SuppressWarnings("unused") public Optional<? extends ITerm> call(IScopeGraphContext<?> context, ITerm term,
            List<ITerm> terms) throws InterpreterException {
        throw new UnsupportedOperationException("Subclasses must override ScopeGraphContextPrimitive::call");
    }

}