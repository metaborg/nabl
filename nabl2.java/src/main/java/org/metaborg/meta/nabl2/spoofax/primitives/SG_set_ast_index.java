package org.metaborg.meta.nabl2.spoofax.primitives;

import org.metaborg.meta.nabl2.stratego.StrategoTermIndices;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class SG_set_ast_index extends AbstractPrimitive {

    public SG_set_ast_index() {
        super(SG_set_ast_index.class.getSimpleName(), 0, 1);
    }

    @Override public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars) throws InterpreterException {
        return StrategoTermIndices.match(tvars[0]).map(index -> {
            env.setCurrent(StrategoTermIndices.put(index, env.current(), env.getFactory()));
            return true;
        }).orElseThrow(() -> new InterpreterException("Term argument is not a TermIndex"));
    }

}