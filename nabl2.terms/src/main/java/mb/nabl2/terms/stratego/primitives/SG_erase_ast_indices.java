package mb.nabl2.terms.stratego.primitives;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

import mb.nabl2.terms.stratego.StrategoTermIndices;

public class SG_erase_ast_indices extends AbstractPrimitive {

    public SG_erase_ast_indices() {
        super(SG_erase_ast_indices.class.getSimpleName(), 0, 0);
    }

    @Override public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars) throws InterpreterException {
        env.setCurrent(StrategoTermIndices.erase(env.current(), env.getFactory()));
        return true;
    }

}