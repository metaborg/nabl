package mb.nabl2.terms.stratego.primitives;

import mb.nabl2.terms.stratego.StrategoTermIndices;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.util.TermUtils;

public class SG_index_more_ast extends AbstractPrimitive {

    public SG_index_more_ast() {
        super(SG_index_more_ast.class.getSimpleName(), 0, 1);
    }

    @Override public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars) throws InterpreterException {
        env.setCurrent(StrategoTermIndices.indexMore(env.current(), TermUtils.toJavaString(tvars[0]), env.getFactory()));
        return true;
    }

}
