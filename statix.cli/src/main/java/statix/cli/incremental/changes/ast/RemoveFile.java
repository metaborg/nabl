package statix.cli.incremental.changes.ast;

import static statix.cli.MStatix.S;

import java.io.File;

import org.spoofax.interpreter.terms.IStrategoTerm;

import statix.cli.StatixData;
import statix.cli.incremental.changes.IIncrementalASTChange;
import statix.cli.incremental.changes.IncrementalChange;

public class RemoveFile extends IIncrementalASTChange {
    public static final RemoveFile instance = new RemoveFile();
    
    private RemoveFile() {
        super("file", "remove");
    }
    
    private RemoveFile(String args) {
        super("file", "remove", args);
    }

    @Override
    public IStrategoTerm apply(StatixData data, IStrategoTerm ast) {
        return S.termFactoryService.getGeneric().makeTuple();
    }
    
    @Override
    public IncrementalChange withArguments(String args) {
        File file = new File(args);
        if (!file.exists()) throw new IllegalArgumentException("The file " + file + " does not exist!");
        return new RemoveFile(args);
    }
    
    @Override
    public boolean hasFile() {
        return arguments != null;
    }

}
