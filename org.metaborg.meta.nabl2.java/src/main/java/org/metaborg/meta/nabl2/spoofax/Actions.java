package org.metaborg.meta.nabl2.spoofax;

import java.util.Collection;
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

    private final IStrategoConstructor analyzeInitial;
    private final IStrategoConstructor analyzeUnit;
    private final IStrategoConstructor analyzeFinal;

    private final IStrategoConstructor params;
    private final IStrategoConstructor paramsAndType;

    private final IStrategoConstructor customInitial;
    private final IStrategoConstructor customUnit;
    private final IStrategoConstructor customFinal;


    public Actions(ITermFactory termFactory, StrategoTerms strategoTerms) {
        this.termFactory = termFactory;

        this.analyzeInitial = termFactory.makeConstructor("AnalyzeInitial", 1);
        this.analyzeUnit = termFactory.makeConstructor("AnalyzeUnit", 3);
        this.analyzeFinal = termFactory.makeConstructor("AnalyzeFinal", 1);

        this.params = termFactory.makeConstructor("Params", 1);
        this.paramsAndType = termFactory.makeConstructor("ParamsAndType", 2);

        this.customInitial = termFactory.makeConstructor("CustomInitial", 1);
        this.customUnit = termFactory.makeConstructor("CustomUnit", 3);
        this.customFinal = termFactory.makeConstructor("CustomFinal", 3);

        this.strategoTerms = strategoTerms;
    }

    public IStrategoTerm analyzeInitial(String resource) {
        return termFactory.makeAppl(analyzeInitial, sourceTerm(resource));
    }

    public IStrategoTerm analyzeUnit(String resource, IStrategoTerm ast, Args args) {
        return termFactory.makeAppl(analyzeUnit, sourceTerm(resource), ast, args(args));
    }

    public IStrategoTerm analyzeFinal(String resource) {
        return termFactory.makeAppl(analyzeFinal, sourceTerm(resource));
    }


    public IStrategoTerm customInitial(String resource) {
        return termFactory.makeAppl(customInitial, sourceTerm(resource));
    }

    public IStrategoTerm customUnit(String resource, IStrategoTerm ast, IStrategoTerm initial) {
        return termFactory.makeAppl(customUnit, sourceTerm(resource), ast, initial);
    }

    public IStrategoTerm customFinal(String resource, IStrategoTerm initial, Collection<IStrategoTerm> units) {
        return termFactory.makeAppl(customFinal, sourceTerm(resource), initial, termFactory.makeList(units));
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