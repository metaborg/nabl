package statix.cli.incremental.changes.ast;

import static statix.cli.MStatix.S;
import static statix.cli.StrategoUtil.*;

import org.spoofax.interpreter.terms.IStrategoTerm;

import statix.cli.StatixData;
import statix.cli.incremental.changes.IIncrementalASTChange;

public class RemoveMethod extends IIncrementalASTChange {
    public static final RemoveMethod instance = new RemoveMethod();
    
    private RemoveMethod() {
        super("method", "remove");
    }

    @Override
    public IStrategoTerm apply(StatixData data, IStrategoTerm ast) {
        return alterClass(S, ast, clazz -> alterMethod(S, clazz, m -> null));
    }

}
