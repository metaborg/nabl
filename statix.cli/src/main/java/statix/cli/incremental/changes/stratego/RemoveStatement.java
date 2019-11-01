package statix.cli.incremental.changes.stratego;

import statix.cli.incremental.changes.IIncrementalStrategoChange;

public class RemoveStatement extends IIncrementalStrategoChange {
    public static final RemoveStatement instance = new RemoveStatement();
    
    private RemoveStatement() {
        super("statement", "remove");
    }
    
    @Override
    public boolean hasNumbers() {
        return true;
    }
    
    @Override
    public String strategy() {
        return "eval-remove-statement";
    }
}
