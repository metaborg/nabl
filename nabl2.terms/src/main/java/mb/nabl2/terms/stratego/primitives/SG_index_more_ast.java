package mb.nabl2.terms.stratego.primitives;

import mb.nabl2.terms.stratego.StrategoTermIndices;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.util.TermUtils;

/**
 * Assigns unique AST indices to the terms of the given subtree that have no index assigned already.
 * <p>
 * The first argument is the resource name, which can be an empty string.
 */
public final class SG_index_more_ast extends AbstractPrimitive {

    public SG_index_more_ast() {
        super(SG_index_more_ast.class.getSimpleName(), 0, 2);
    }

    @Override public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars) throws InterpreterException {
        final String resource = TermUtils.toJavaString(tvars[0]);
        final int startIndex = TermUtils.toJavaInt(tvars[1]);
        env.setCurrent(StrategoTermIndices.indexMore(env.current(), resource, startIndex, env.getFactory()));
        return true;
    }

}
