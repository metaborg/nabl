package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
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

public abstract class ScopeGraphPrimitive extends AbstractPrimitive {

    private static ILogger logger = LoggerUtils.logger(ScopeGraphPrimitive.class);

    public ScopeGraphPrimitive(String name, int svars, int tvars) {
        super(name, svars, tvars);
    }

    @Override public final boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars)
            throws InterpreterException {
        final Object contextObj = env.contextObject();
        if (contextObj == null) {
            logger.warn("Context is null.");
            return false;
        }
        if (!(contextObj instanceof IScopeGraphContext)) {
            throw new InterpreterException("Context does not implement IScopeGraphContext");
        }
        final IScopeGraphContext<?> context = (IScopeGraphContext<?>) env.contextObject();
        StrategoTerms strategoTerms = new StrategoTerms(env.getFactory());
        List<ITerm> termArgs = Arrays.asList(tvars).stream().map(strategoTerms::fromStratego).collect(Collectors
                .toList());
        Optional<? extends ITerm> result;
        try (IClosableLock lock = context.guard()) {
            result = call(context, strategoTerms.fromStratego(env.current()), termArgs);
        }
        return result.map(t -> {
            env.setCurrent(strategoTerms.toStratego(t));
            return true;
        }).orElse(false);
    }

    public abstract Optional<? extends ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> termVars)
            throws InterpreterException;

}
