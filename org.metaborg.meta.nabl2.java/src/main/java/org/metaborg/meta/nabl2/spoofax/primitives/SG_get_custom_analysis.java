package org.metaborg.meta.nabl2.spoofax.primitives;

import org.metaborg.meta.nabl2.spoofax.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.StrategoTermIndex;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class SG_get_custom_analysis extends ScopeGraphPrimitive {

    public SG_get_custom_analysis() {
        super(SG_get_custom_analysis.class.getSimpleName(), 0, 0);
    }

    @Override public boolean call(IScopeGraphContext<?> context, IContext env, Strategy[] strategies,
            IStrategoTerm[] terms) throws InterpreterException {
        StrategoTermIndex strategoIndex = StrategoTermIndex.get(env.current());
        if (strategoIndex == null) {
            return false;
        }
        return context.unit(strategoIndex.getResource()).solution().flatMap(s -> s.getCustom()).map(s -> {
            env.setCurrent(s);
            return true;
        }).orElse(false);
    }

}