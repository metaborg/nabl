package statix.cli.incremental.changes;

import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

import statix.cli.StatixAnalyze;
import statix.cli.StatixData;
import statix.cli.StatixParse;
import statix.cli.TestRandomness;

public abstract class IIncrementalStrategoChange extends IncrementalChange implements IDesugaredOutput {
    public IIncrementalStrategoChange(String group, String sort) {
        super(group, sort);
    }
    
    protected IIncrementalStrategoChange(String group, String sort, String args) {
        super(group, sort, args);
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
    public abstract String strategy();
    
    public abstract boolean hasNumbers();
    
    @Override
    public ISpoofaxParseUnit parse(StatixData data, StatixParse parse, StatixAnalyze analyze, TestRandomness random, String file) throws MetaborgException {
        ISpoofaxParseUnit original = analyze.desugarAst(parse.parse(file));
        if (hasNumbers()) {
            return analyze.applyStrategoTransformation(original, strategy(), random.getRandom().nextInt(1000), random.getRandom().nextInt(1000));
        } else {
            return analyze.applyStrategoTransformation(original, strategy());
        }
    }
}
