package org.metaborg.meta.nabl2.spoofax.primitives;

import org.metaborg.meta.nabl2.stratego.StrategoTermIndices;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class SG_index_ast extends AbstractPrimitive {

    public SG_index_ast() {
        super(SG_index_ast.class.getSimpleName(), 0, 1);
    }

    @Override public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars) throws InterpreterException {
        env.setCurrent(StrategoTermIndices.index(env.current(), Tools.asJavaString(tvars[0]), env.getFactory()));
        return true;
    }

}