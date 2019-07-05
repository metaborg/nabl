package statix.cli.incremental.changes.ast;

import static statix.cli.MStatix.S;
import static statix.cli.StrategoUtil.*;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;

import statix.cli.StatixData;
import statix.cli.incremental.changes.IIncrementalASTChange;

public class RenameMethod extends IIncrementalASTChange {
    public static final RenameMethod instance = new RenameMethod();
    
    private RenameMethod() {
        super("method", "rename");
    }

    @Override
    public IStrategoTerm apply(StatixData data, IStrategoTerm ast) {
        return alterClass(S, ast, clazz -> transform(data, clazz));
    }
    
    public IStrategoAppl transform(StatixData data, IStrategoAppl clazz) {
        return alterMethod(S, clazz, method -> setMethodName(S, method, data.freshName()));
    }

}
