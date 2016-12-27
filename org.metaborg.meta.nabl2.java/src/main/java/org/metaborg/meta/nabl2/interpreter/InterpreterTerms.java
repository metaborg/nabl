package org.metaborg.meta.nabl2.interpreter;

import org.metaborg.meta.nabl2.solver.ISolution;
import org.metaborg.meta.nabl2.stratego.StrategoTerms;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.ITermFactory;

public class InterpreterTerms {

    private final ITermFactory termFactory;
    private final StrategoTerms strategoTerms;

    public InterpreterTerms(ITermFactory termFactory) {
        this.termFactory = termFactory;
        this.strategoTerms = new StrategoTerms(termFactory);
    }

    public IStrategoTerm context(ISolution solution) {
        return appl("NaBL2");
    }

    private IStrategoAppl appl(String op, IStrategoTerm... args) {
        return termFactory.makeAppl(termFactory.makeConstructor(op, args.length), args);
    }

    private IStrategoTuple tuple(IStrategoTerm... args) {
        return termFactory.makeTuple(args);
    }

}