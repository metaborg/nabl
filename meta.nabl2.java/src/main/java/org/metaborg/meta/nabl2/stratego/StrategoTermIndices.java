package org.metaborg.meta.nabl2.stratego;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

public class StrategoTermIndices {

    private static final String OP = "TermIndex";
    private static final int ARITY = 2;

    // index

    public static IStrategoTerm index(IStrategoTerm term, String resource, ITermFactory termFactory) {
        return new Indexer(resource, termFactory).index(term);
    }

    private static class Indexer {

        private final String resource;
        private final ITermFactory termFactory;

        private int currentId = 0;

        Indexer(String resource, ITermFactory termFactory) {
            super();
            this.resource = resource;
            this.termFactory = termFactory;
        }

        private IStrategoTerm index(final IStrategoTerm term) {
            IStrategoTerm result = StrategoTerms.match(term,
                    StrategoTerms.<IStrategoTerm>cases(
                            // @formatter:off
                            appl -> termFactory.makeAppl(appl.getConstructor(), index(appl.getAllSubterms()),
                                    appl.getAnnotations()),
                            tuple -> termFactory.makeTuple(index(tuple.getAllSubterms()), tuple.getAnnotations()),
                            list -> index(list),
                            integer -> termFactory.makeInt(integer.intValue()),
                            real -> termFactory.makeReal(real.realValue()),
                            string -> termFactory.makeString(string.stringValue())
                    // @formatter:on
                    ));
            result = put(ImmutableTermIndex.of(resource, ++currentId), result, termFactory);
            termFactory.copyAttachments(term, result);
            return result;
        }

        private IStrategoList index(final IStrategoList list) {
            IStrategoList result;
            if (list.isEmpty()) {
                result = termFactory.makeList(new IStrategoTerm[0], list.getAnnotations());
            } else {
                result = termFactory.makeListCons(index(list.head()), index(list.tail()), list.getAnnotations());
            }
            result = (IStrategoList) put(ImmutableTermIndex.of(resource, ++currentId), result, termFactory);
            termFactory.copyAttachments(list, result);
            return result;
        }

        private IStrategoTerm[] index(final IStrategoTerm[] terms) {
            return Arrays.asList(terms).stream().map(this::index).toArray(n -> new IStrategoTerm[n]);
        }

    }

    // erase

    public static IStrategoTerm erase(IStrategoTerm term, ITermFactory termFactory) {
        return new Eraser(termFactory).erase(term);

    }

    private static class Eraser {

        private final ITermFactory termFactory;

        Eraser(ITermFactory termFactory) {
            this.termFactory = termFactory;
        }

        private IStrategoTerm erase(final IStrategoTerm term) {
            IStrategoTerm result = StrategoTerms.match(term,
                    StrategoTerms.<IStrategoTerm>cases(
                            // @formatter:off
                            appl -> termFactory.makeAppl(appl.getConstructor(), erase(appl.getAllSubterms()),
                                    appl.getAnnotations()),
                            tuple -> termFactory.makeTuple(erase(tuple.getAllSubterms()), tuple.getAnnotations()),
                            list -> erase(list), integer -> termFactory.makeInt(integer.intValue()),
                            real -> termFactory.makeReal(real.realValue()),
                            string -> termFactory.makeString(string.stringValue())
                    // @formatter:on
                    ));
            result = remove(result, termFactory);
            termFactory.copyAttachments(term, result);
            assert !get(result).isPresent();
            return result;
        }

        private IStrategoList erase(final IStrategoList list) {
            IStrategoList result;
            if (list.isEmpty()) {
                result = termFactory.makeList(new IStrategoTerm[0], list.getAnnotations());
            } else {
                result = termFactory.makeListCons(erase(list.head()), erase(list.tail()), list.getAnnotations());
            }
            result = (IStrategoList) remove(result, termFactory);
            termFactory.copyAttachments(list, result);
            assert !get(result).isPresent();
            return result;
        }

        private IStrategoTerm[] erase(final IStrategoTerm[] terms) {
            return Arrays.asList(terms).stream().map(this::erase).toArray(n -> new IStrategoTerm[n]);
        }

    }

    // indices of terms

    public static Optional<TermIndex> get(IStrategoTerm term) {
        for (IStrategoTerm anno : term.getAnnotations()) {
            Optional<TermIndex> index = match(anno);
            if (index.isPresent()) {
                return index;
            }
        }
        return Optional.empty();
    }

    public static <T extends IStrategoTerm> T put(ITermIndex index, T term, ITermFactory factory) {
        @SuppressWarnings({ "unchecked" })
        T result = (T) factory.annotateTerm(term,
                factory.makeListCons(build(index, factory), removeFromAnnoList(term.getAnnotations(), factory)));
        return result;
    }

    public static <T extends IStrategoTerm> T remove(T term, ITermFactory factory) {
        @SuppressWarnings({ "unchecked" })
        T result = (T) factory.annotateTerm(term, removeFromAnnoList(term.getAnnotations(), factory));
        return result;
    }

    // index terms

    public static IStrategoTerm build(ITermIndex index, ITermFactory factory) {
        IStrategoConstructor ctor = factory.makeConstructor(OP, ARITY);
        return factory.makeAppl(ctor, factory.makeString(index.getResource()), factory.makeInt(index.getId()));
    }

    public static Optional<TermIndex> match(IStrategoTerm term) {
        if (!(Tools.isTermAppl(term) && Tools.hasConstructor((IStrategoAppl) term, OP, ARITY))) {
            return Optional.empty();
        }
        IStrategoTerm resourceTerm = term.getSubterm(0);
        IStrategoTerm idTerm = term.getSubterm(1);
        if (!(Tools.isTermString(resourceTerm) && Tools.isTermInt(idTerm))) {
            return Optional.empty();
        }
        return Optional.of(ImmutableTermIndex.of(Tools.asJavaString(resourceTerm), Tools.asJavaInt(idTerm)));
    }

    private static IStrategoList removeFromAnnoList(IStrategoList list, ITermFactory factory) {
        return factory.makeList(Arrays.asList(list.getAllSubterms()).stream().filter(term -> !match(term).isPresent())
                .collect(Collectors.toList()));
    }

}