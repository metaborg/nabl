package statix.cli.incremental.changes;

import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.spoofax.interpreter.terms.IStrategoTerm;

import statix.cli.StatixData;
import statix.cli.StatixParse;
import statix.cli.TestRandomness;

/**
 * Interface to represent an AST based transformation.
 */
public abstract class IIncrementalASTChange extends IncrementalChange {
    public IIncrementalASTChange(String group, String sort) {
        super(group, sort);
    }

    /**
     * Applies this AST transformation to the given AST term.
     * 
     * @param data
     *      the statix data
     * @param ast
     *      the AST to transform
     * 
     * @return
     *      the transformed AST
     * 
     * @throws NotApplicableException
     *      If the given AST cannot be transformed by this transformation.
     */
    public abstract IStrategoTerm apply(StatixData data, IStrategoTerm ast) throws NotApplicableException;
    
    @Override
    public ISpoofaxParseUnit parse(StatixData data, StatixParse parse, TestRandomness random, String file) throws MetaborgException {
        ISpoofaxParseUnit original = parse.parse(file);
        IStrategoTerm newAst = apply(data, original.ast());
        return parse.replace(original, newAst);
    }
}
