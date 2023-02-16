package mb.nabl2.terms.stratego.primitives;

import mb.nabl2.terms.stratego.StrategoTermIndices;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.util.TermUtils;

/**
 * Finds the maximum AST index used in the given subtree;
 * or fails if no AST index was found.
 */
public final class SG_get_max_ast_index extends AbstractPrimitive {

    public SG_get_max_ast_index() {
        super(SG_get_max_ast_index.class.getSimpleName(), 0, 0);
    }

    @Override public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars) throws InterpreterException {
        final int maxIndex = StrategoTermIndices.findMaxIndex(env.current());
        env.setCurrent(env.getFactory().makeInt(maxIndex));
        return maxIndex >= 0;       // Fail if no maxIndex was found
    }

}
