package statix.cli.incremental.changes.ast;

import static statix.cli.MStatix.S;
import static statix.cli.StrategoUtil.alterClasslike;

import org.spoofax.interpreter.terms.IStrategoTerm;

import statix.cli.StatixData;
import statix.cli.incremental.changes.IIncrementalASTChange;

public class RemoveClass extends IIncrementalASTChange {
    public static final RemoveClass instance = new RemoveClass();
    
    private RemoveClass() {
        super("class", "remove");
    }

    @Override
    public IStrategoTerm apply(StatixData data, IStrategoTerm ast) {
        return alterClasslike(S, ast, clazz -> null);
    }

}
