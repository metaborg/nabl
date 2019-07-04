package statix.cli.incremental.changes.ast;

import static statix.cli.MStatix.S;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;

import statix.cli.StatixData;
import statix.cli.StrategoUtil;
import statix.cli.incremental.changes.IIncrementalASTChange;

public class RenameClass extends IIncrementalASTChange {
    public static final RenameClass instance = new RenameClass();
    
    private RenameClass() {
        super("class", "rename");
    }

    @Override
    public IStrategoTerm apply(StatixData data, IStrategoTerm ast) {
        return StrategoUtil.alterClass(S, ast, clazz -> transform(data, clazz));
    }
    
    public IStrategoAppl transform(StatixData data, IStrategoAppl clazz) {
        return StrategoUtil.setClassName(S, clazz, data.freshName());
    }

}
