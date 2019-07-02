package statix.cli.incremental.changes;

import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * Interface to represent an AST based transformation.
 */
public interface IIncrementalASTChange {
    /**
     * Applies this AST transformation to the given AST term.
     * 
     * @param ast
     *      the AST to transform
     * 
     * @return
     *      the transformed AST
     * 
     * @throws IllegalStateException
     *      If the given AST cannot be transformed by this transformation.
     */
    IStrategoTerm apply(IStrategoTerm ast);
}
