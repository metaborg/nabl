package mb.nabl2.terms.stratego.primitives;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

import mb.nabl2.terms.stratego.StrategoTermIndices;

public class SG_get_ast_index extends AbstractPrimitive {

    public SG_get_ast_index() {
        super(SG_get_ast_index.class.getSimpleName(), 0, 0);
    }

    @Override public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars) throws InterpreterException {
        final IStrategoTerm term = env.current();
        return StrategoTermIndices.get(term).map(index -> {
            final IStrategoTerm indexTerm = StrategoTermIndices.build(index, env.getFactory());
            env.setCurrent(indexTerm);
            return true;
        }).orElse(false);
    }

}