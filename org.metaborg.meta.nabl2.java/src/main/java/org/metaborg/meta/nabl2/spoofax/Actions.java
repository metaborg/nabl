package org.metaborg.meta.nabl2.spoofax;

import java.util.Optional;

import org.metaborg.meta.nabl2.stratego.StrategoTermIndex;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import com.google.common.collect.Lists;

public class Actions {

    private final ITermFactory termFactory;

    private final IStrategoConstructor initialAction;
    private final IStrategoConstructor unitAction;
    private final IStrategoConstructor finalAction;
    private final IStrategoConstructor params;
    private final IStrategoConstructor paramsAndType;

    public Actions(ITermFactory termFactory) {
        this.termFactory = termFactory;
        this.initialAction = termFactory.makeConstructor("AnalyzeInitial", 1);
        this.unitAction = termFactory.makeConstructor("AnalyzeUnit", 3);
        this.finalAction = termFactory.makeConstructor("AnalyzeFinal", 1);
        this.params = termFactory.makeConstructor("Params", 1);
        this.paramsAndType = termFactory.makeConstructor("ParamsAndType", 2);
    }

    public IStrategoTerm initialOf(String resource) {
        return termFactory.makeAppl(initialAction, sourceTerm(resource));
    }

    public IStrategoTerm unitOf(String resource, IStrategoTerm ast, Iterable<IStrategoTerm> params,
            Optional<IStrategoTerm> type) {
        return termFactory.makeAppl(unitAction, sourceTerm(resource), ast, args(params, type));
    }

    public IStrategoTerm finalOf(String resource) {
        return termFactory.makeAppl(finalAction, sourceTerm(resource));
    }

    private IStrategoTerm sourceTerm(String resource) {
        IStrategoString sourceTerm = termFactory.makeString(resource);
        StrategoTermIndex.put(sourceTerm, resource, 0);
        return sourceTerm;
    }

    private IStrategoTerm args(Iterable<IStrategoTerm> params, Optional<IStrategoTerm> type) {
        IStrategoList paramsTerm = termFactory.makeList(Lists.newArrayList(params));
        return type.<IStrategoTerm> map(typeTerm -> termFactory.makeAppl(paramsAndType, paramsTerm, typeTerm))
                .orElseGet(() -> termFactory.makeAppl(this.params, paramsTerm));
    }

}