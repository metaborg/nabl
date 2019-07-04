package statix.cli.incremental.changes.ast;

import static statix.cli.MStatix.S;
import static statix.cli.StrategoUtil.*;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;

import statix.cli.StatixData;
import statix.cli.incremental.changes.IIncrementalASTChange;

public class AddMethod extends IIncrementalASTChange {
    public static final AddMethod instance = new AddMethod();
    
    private AddMethod() {
        super("method", "add");
    }

    @Override
    public IStrategoTerm apply(StatixData data, IStrategoTerm ast) {
        
        alterClass(S, ast, clazz -> transform(data, clazz));
        return ast;
    }
    
    public IStrategoAppl transform(StatixData data, IStrategoAppl clazz) {
        IStrategoList declarations = getDeclarations(clazz);
        IStrategoList newDeclarations = addItemToList(S, declarations, 0, createMethod(S, data.freshName(), typeVoid(S)));
        return replaceDeclarations(S, clazz, newDeclarations);
    }

}
