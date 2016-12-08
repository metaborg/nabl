package org.metaborg.meta.nabl2.spoofax;

import java.util.List;

import org.metaborg.meta.nabl2.stratego.StrategoTermIndex;
import org.metaborg.meta.nabl2.stratego.StrategoTerms;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

public class Actions {

    private final ITermFactory termFactory;
    private final StrategoTerms strategoTerms;

    private final IStrategoConstructor initialAction;
    private final IStrategoConstructor unitAction;
    private final IStrategoConstructor finalAction;
    private final IStrategoConstructor params;
    private final IStrategoConstructor paramsAndType;


    public Actions(ITermFactory termFactory, StrategoTerms strategoTerms) {
        this.termFactory = termFactory;
        this.initialAction = termFactory.makeConstructor("AnalyzeInitial", 1);
        this.unitAction = termFactory.makeConstructor("AnalyzeUnit", 3);
        this.finalAction = termFactory.makeConstructor("AnalyzeFinal", 1);
        this.params = termFactory.makeConstructor("Params", 1);
        this.paramsAndType = termFactory.makeConstructor("ParamsAndType", 2);
        this.strategoTerms = strategoTerms;
    }

    public IStrategoTerm initialOf(String resource) {
        return termFactory.makeAppl(initialAction, sourceTerm(resource));
    }

    public IStrategoTerm unitOf(String resource, IStrategoTerm ast, Args args) {
        return termFactory.makeAppl(unitAction, sourceTerm(resource), ast, args(args));
    }

    public IStrategoTerm finalOf(String resource) {
        return termFactory.makeAppl(finalAction, sourceTerm(resource));
    }

    private IStrategoTerm sourceTerm(String resource) {
        IStrategoString sourceTerm = termFactory.makeString(resource);
        StrategoTermIndex.put(sourceTerm, resource, 0);
        return sourceTerm;
    }

    private IStrategoTerm args(Args args) {
        List<IStrategoTerm> paramTerms = strategoTerms.toStrategos(args.getParams());
        IStrategoTerm paramsTerm;
        if (paramTerms.size() == 1) {
            paramsTerm = paramTerms.get(0);
        } else {
            paramsTerm = termFactory.makeTuple(paramTerms.toArray(new IStrategoTerm[0]));
        }
        return args.getType()
                // @formatter:off
                .map(typeTerm -> termFactory.makeAppl(paramsAndType, paramsTerm, strategoTerms.toStratego(typeTerm)))
                .orElseGet(() -> termFactory.makeAppl(this.params, paramsTerm));
                // @formatter:on
    }

}