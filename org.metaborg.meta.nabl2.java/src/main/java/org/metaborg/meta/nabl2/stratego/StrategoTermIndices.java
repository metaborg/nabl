package org.metaborg.meta.nabl2.stratego;

import java.util.Arrays;

import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

public class StrategoTermIndices {

    private final ITermFactory termFactory;
    private final String resource;
    private int currentId;
 
    public StrategoTermIndices(String resource, ITermFactory termFactory) {
        super();
        this.termFactory = termFactory;
        this.resource = resource;
        this.currentId = 0;
    }

    private IStrategoTerm indexTerm(final IStrategoTerm term) {
        IStrategoTerm result = StrategoTerms.match(term, StrategoTerms.<IStrategoTerm> cases(
            appl -> termFactory.makeAppl(appl.getConstructor(), indexTerms(appl.getAllSubterms()), appl.getAnnotations()),
            tuple -> termFactory.makeTuple(tuple.getAllSubterms(), tuple.getAnnotations()),
            list -> indexList(list),
            integer -> termFactory.makeInt(integer.intValue()),
            real -> termFactory.makeReal(real.realValue()),
            string -> termFactory.makeString(string.stringValue())
        ));
        termFactory.copyAttachments(term, result);
        assert StrategoTermIndex.get(result) == null;
        StrategoTermIndex.put(result, resource, ++currentId);
        return result;
    }
 
    private IStrategoList indexList(final IStrategoList list) {
        IStrategoList result;
        if (list.isEmpty()) {
            result = termFactory.makeList(new IStrategoTerm[0], list.getAnnotations());
        } else {
            result = termFactory.makeListCons(indexTerm(list.head()), indexList(list.tail()), list.getAnnotations());
        }
        termFactory.copyAttachments(list, result);
        assert StrategoTermIndex.get(result) == null;
        StrategoTermIndex.put(result, resource, ++currentId);
        return result;
    }

    private IStrategoTerm[] indexTerms(final IStrategoTerm[] terms) {
        return Arrays.asList(terms).stream().map(this::indexTerm).toArray(n -> new IStrategoTerm[n]);
    }

    public static IStrategoTerm indexTerm(IStrategoTerm term, String resource, ITermFactory termFactory) {
        return new StrategoTermIndices(resource, termFactory).indexTerm(term);
    }
 
}