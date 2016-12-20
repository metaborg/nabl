package org.metaborg.meta.nabl2.spoofax.primitives;

import org.metaborg.meta.nabl2.ScopeGraphException;
import org.metaborg.meta.nabl2.stratego.StrategoTermIndices;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class SG_index_sublist extends AbstractPrimitive {

    public SG_index_sublist() {
        super(SG_index_sublist.class.getSimpleName(), 0, 1);
    }

    @Override public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars) throws InterpreterException {
        try {
            StrategoTermIndices.indexSublist(tvars[0], env.current());
        } catch (ScopeGraphException e) {
            throw new InterpreterException(e);
        }
        return true;
    }

}