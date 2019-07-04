package statix.cli.incremental.changes.ast;

import static statix.cli.MStatix.S;

import org.spoofax.interpreter.terms.IStrategoTerm;

import statix.cli.StatixData;
import statix.cli.incremental.changes.IIncrementalASTChange;

public class RemoveFile extends IIncrementalASTChange {
    public static final RemoveFile instance = new RemoveFile();
    
    private RemoveFile() {
        super("file", "remove");
    }

    @Override
    public IStrategoTerm apply(StatixData data, IStrategoTerm ast) {
        return S.termFactoryService.getGeneric().makeTuple();
    }

}
