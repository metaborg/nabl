package statix.cli.incremental.changes.ast;

import static statix.cli.MStatix.S;

import org.spoofax.interpreter.terms.IStrategoTerm;

import statix.cli.StatixData;
import statix.cli.incremental.changes.IIncrementalASTChange;

/**
 * Does not change the AST, but prints it instead.
 */
public class NopPrint extends IIncrementalASTChange {
    public static final NopPrint instance = new NopPrint();
    
    private NopPrint() {
        super("nop", "print");
        ALL.remove("*", "*", this);
    }

    @Override
    public IStrategoTerm apply(StatixData data, IStrategoTerm ast) {
        System.out.println("AST:  " + S.strategoCommon.prettyPrint(ast).stringValue());
        return ast;
    }
    
}
