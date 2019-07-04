package statix.cli.incremental.changes.ast;

import static statix.cli.MStatix.S;

import org.spoofax.interpreter.terms.IStrategoTerm;

import statix.cli.StatixData;
import statix.cli.StrategoUtil;
import statix.cli.incremental.changes.IIncrementalASTChange;

public class RemoveMethod extends IIncrementalASTChange {
    public static final RemoveMethod instance = new RemoveMethod();
    
    private RemoveMethod() {
        super("method", "remove");
    }

    @Override
    public IStrategoTerm apply(StatixData data, IStrategoTerm ast) {
        return StrategoUtil.alterClass(S, ast, clazz -> StrategoUtil.alterMethod(S, clazz, m -> null));
    }

}
