package mb.nabl2.spoofax.primitives;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;

import mb.nabl2.spoofax.analysis.IScopeGraphContext;
import mb.nabl2.stratego.StrategoTermIndices;

public final class PrimitiveUtil {

    public PrimitiveUtil() {
    }

    public static IScopeGraphContext<?> scopeGraphContext(IContext env) throws InterpreterException {
        final Object contextObj = env.contextObject();
        if(contextObj == null) {
            throw new InterpreterException("No context present.");
        }
        if(!(contextObj instanceof IScopeGraphContext)) {
            throw new InterpreterException("Context does not implement IScopeGraphContext");
        }
        final IScopeGraphContext<?> context = (IScopeGraphContext<?>) env.contextObject();
        return context;
    }

    /**
     * @deprecated Removed after next bootstrapping cycle
     */
    public static boolean isAnalysisToken(IStrategoTerm term) {
        return Tools.isTermAppl(term) && Tools.hasConstructor((IStrategoAppl) term, "AnalysisToken", 0);
    }

    /**
     * @deprecated Removed after next bootstrapping cycle
     */
    public static String getAnalysisToken(IStrategoTerm term) {
        if(!isAnalysisToken(term)) {
            throw new IllegalArgumentException("Expected analysis token, got " + term);
        }
        return StrategoTermIndices.get(term).map(idx -> idx.getResource())
                .orElseThrow(() -> new IllegalArgumentException("Not a valid analysis token."));
    }

}