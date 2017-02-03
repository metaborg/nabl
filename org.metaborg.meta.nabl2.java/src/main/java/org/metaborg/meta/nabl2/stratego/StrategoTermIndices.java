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
        IStrategoList annos = makeAnnos(term);
        IStrategoTerm result = StrategoTerms.match(term, StrategoTerms.<IStrategoTerm> cases(
            appl -> termFactory.makeAppl(appl.getConstructor(), indexTerms(appl.getAllSubterms())),
            tuple -> termFactory.makeTuple(tuple.getAllSubterms()),
            list -> indexList(list),
            integer -> integer,
            real -> real,
            string -> string,
            ref -> ref,
            placeholder -> placeholder,
            other -> other
        ));
        return termFactory.copyAttachments(term, termFactory.annotateTerm(result, annos));
    }
 
    private IStrategoList indexList(final IStrategoList list) {
        IStrategoList annos = makeAnnos(list);
        IStrategoTerm result;
        if (list.isEmpty()) {
            result = termFactory.makeList(new IStrategoTerm[0], annos);
        } else {
            result = termFactory.makeListCons(indexTerm(list.head()), indexList(list.tail()), annos);
        }
        return (IStrategoList) termFactory.copyAttachments(list, termFactory.annotateTerm(result, annos));
    }

    private IStrategoTerm[] indexTerms(final IStrategoTerm[] terms) {
        return Arrays.asList(terms).stream().map(this::indexTerm).toArray(n -> new IStrategoTerm[n]);
    }

    private IStrategoList makeAnnos(IStrategoTerm term) {
        IStrategoTerm index = StrategoTermIndex.of(resource, ++currentId, termFactory);
        return termFactory.makeListCons(index, term.getAnnotations());
    }

    public static IStrategoTerm indexTerm(IStrategoTerm term, String resource, ITermFactory termFactory) {
        return new StrategoTermIndices(resource, termFactory).indexTerm(term);
    }
 
}