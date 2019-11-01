package statix.cli.util;

import java.util.function.Function;

import org.spoofax.interpreter.terms.IStrategoAppl;

import statix.cli.incremental.changes.NotApplicableException;

public interface ITransformation extends Function<IStrategoAppl, IStrategoAppl> {
    /**
     * Applies this transformation to the given term.
     * 
     * @param term
     *      the term
     * 
     * @return
     *      the altered term
     * 
     * @throws NotApplicableException
     *      If this transformation cannot be applied to the given term
     */
    @Override
    IStrategoAppl apply(IStrategoAppl term) throws NotApplicableException;
}
