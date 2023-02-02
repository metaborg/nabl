package mb.nabl2.terms.stratego;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.TermFactory;
import org.spoofax.terms.util.TermUtils;

public final class StrategoTermIndices {

    // Prevent instantiation.
    private StrategoTermIndices() {
    }

    private static final String OP = "TermIndex";
    private static final int ARITY = 2;

    // index

    public static IStrategoTerm index(IStrategoTerm term, String resource, ITermFactory termFactory) {
        return new Indexer(resource, termFactory, true, 1).index(term);
    }

    public static IStrategoTerm indexMore(IStrategoTerm term, String resource, ITermFactory termFactory) {
        int maxId = findMaxId(term);
        if (maxId >= 0) {
            return new Indexer(resource, termFactory, false, maxId + 1).index(term);
        } else {
            return index(term, resource, termFactory);
        }
    }
    private static int findMaxId(final IStrategoTerm term) {
        int maxId = get(term).map(TermIndex::getId).orElse(-1);
        for(IStrategoTerm subterm : term.getSubterms()) {
            maxId = Math.max(maxId, findMaxId(subterm));
        }
        return maxId;
    }
    private static class Indexer {

        private final String resource;
        private final ITermFactory termFactory;

        private int nextId;
        private boolean overwrite;
        public Indexer(String resource, ITermFactory termFactory, boolean overwrite, int initialId) {
            this.resource = resource;
            this.termFactory = termFactory;
            this.overwrite = overwrite;
            this.nextId = initialId;
        }

        private IStrategoTerm index(final IStrategoTerm term) {
            // @formatter:off
            IStrategoTerm result = StrategoTerms.match(term,
                StrategoTerms.<IStrategoTerm>cases(
                    appl -> termFactory.makeAppl(appl.getConstructor(), index(appl.getSubterms()), appl.getAnnotations()),
                    tuple -> termFactory.makeTuple(index(tuple.getSubterms()), tuple.getAnnotations()),
                    list -> index(list),
                    integer -> termFactory.annotateTerm(termFactory.makeInt(integer.intValue()), integer.getAnnotations()),
                    real -> termFactory.annotateTerm(termFactory.makeReal(real.realValue()), real.getAnnotations()),
                    string -> termFactory.annotateTerm(termFactory.makeString(string.stringValue()), string.getAnnotations()),
                    blob -> new StrategoBlob(blob.value()),
                    plhdr -> termFactory.annotateTerm(termFactory.makePlaceholder(plhdr.getTemplate()), plhdr.getAnnotations())
                ));
            // @formatter:on
            if(overwrite || !get(term).isPresent()) {
                // Associate the term's origin, if any, with the new term index
                final TermIndex index1 = TermIndex.of(resource, nextId++);
                final TermIndex index = TermOrigin.get(term).map(o -> o.put(index1)).orElse(index1);
                result = put(index, result, termFactory);
            }
            termFactory.copyAttachments(term, result);
            return result;
        }

        private IStrategoList index(final IStrategoList list) {
            IStrategoList result;
            if(list.isEmpty()) {
                result = termFactory.makeList(TermFactory.EMPTY_TERM_ARRAY, list.getAnnotations());
            } else {
                result = termFactory.makeListCons(index(list.head()), index(list.tail()), list.getAnnotations());
            }

            // Set the term index
            if(overwrite || !get(list).isPresent()) {
                // Associate the term's origin, if any, with the new term index
                final TermIndex index1 = TermIndex.of(resource, nextId++);
                final TermIndex index = TermOrigin.get(list).map(o -> o.put(index1)).orElse(index1);
                result = put(index, result, termFactory);
            }

            // Copy the original term's attachments to the new term
            termFactory.copyAttachments(list, result);
            return result;
        }

        private IStrategoTerm[] index(final List<IStrategoTerm> terms) {
            return terms.stream().map(this::index).toArray(n -> new IStrategoTerm[0]);
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
            IStrategoTerm result = StrategoTerms.match(term, StrategoTerms.<IStrategoTerm>cases(
            // @formatter:off
                            appl -> termFactory.makeAppl(appl.getConstructor(), erase(appl.getSubterms()),
                                    appl.getAnnotations()),
                            tuple -> termFactory.makeTuple(erase(tuple.getSubterms()), tuple.getAnnotations()),
                            list -> erase(list),
                            integer -> termFactory.annotateTerm(termFactory.makeInt(integer.intValue()), integer.getAnnotations()),
                            real -> termFactory.annotateTerm(termFactory.makeReal(real.realValue()), real.getAnnotations()),
                            string -> termFactory.annotateTerm(termFactory.makeString(string.stringValue()), string.getAnnotations()),
                            blob -> new StrategoBlob(blob.value()),
                            plhdr -> termFactory.annotateTerm(termFactory.makePlaceholder(plhdr.getTemplate()), plhdr.getAnnotations())
                    // @formatter:on
            ));
            termFactory.copyAttachments(term, result);
            result = remove(result, termFactory);
            assert !get(result).isPresent();
            return result;
        }

        private IStrategoList erase(final IStrategoList list) {
            IStrategoList result;
            if(list.isEmpty()) {
                result = termFactory.makeList(TermFactory.EMPTY_TERM_ARRAY, list.getAnnotations());
            } else {
                result = termFactory.makeListCons(erase(list.head()), erase(list.tail()), list.getAnnotations());
            }
            termFactory.copyAttachments(list, result);
            result = (IStrategoList) remove(result, termFactory);
            assert !get(result).isPresent();
            return result;
        }

        private IStrategoTerm[] erase(final List<IStrategoTerm> terms) {
            return terms.stream().map(this::erase).toArray(n -> new IStrategoTerm[0]);
        }

    }

    // indices of terms

    public static Optional<TermIndex> get(IStrategoTerm term) {
        for(IStrategoTerm anno : term.getAnnotations()) {
            Optional<TermIndex> index = match(anno);
            if(index.isPresent()) {
                return index;
            }
        }
        return Optional.empty();
    }

    public static <T extends IStrategoTerm> T put(TermIndex index, T term, ITermFactory factory) {
        @SuppressWarnings({ "unchecked" }) T result = (T) factory.annotateTerm(term,
                factory.makeListCons(build(index, factory), removeFromAnnoList(term.getAnnotations(), factory)));
        return result;
    }

    public static <T extends IStrategoTerm> T remove(T term, ITermFactory factory) {
        @SuppressWarnings({ "unchecked" }) T result =
                (T) factory.annotateTerm(term, removeFromAnnoList(term.getAnnotations(), factory));
        return result;
    }

    // index terms

    public static IStrategoTerm build(TermIndex index, ITermFactory factory) {
        final IStrategoConstructor ctor = factory.makeConstructor(OP, ARITY);
        final IStrategoTerm indexTerm =
                factory.makeAppl(ctor, factory.makeString(index.getResource()), factory.makeInt(index.getId()));
        TermOrigin.get(index).ifPresent(o -> o.put(indexTerm));
        return indexTerm;
    }

    public static Optional<TermIndex> match(IStrategoTerm term) {
        if(!TermUtils.isAppl(term, OP, ARITY)) {
            return Optional.empty();
        }
        IStrategoTerm resourceTerm = term.getSubterm(0);
        IStrategoTerm idTerm = term.getSubterm(1);

        final TermIndex index1 = TermIndex.of(TermUtils.toJavaString(resourceTerm), TermUtils.toJavaInt(idTerm));
        final TermIndex index2 = (TermIndex) TermOrigin.get(term).map(o -> o.put(index1)).orElse(index1);
        return Optional.of(index2);
    }

    private static IStrategoList removeFromAnnoList(IStrategoList list, ITermFactory factory) {
        List<IStrategoTerm> terms = new ArrayList<>(list.getAllSubterms().length);
        for(IStrategoTerm term : list.getAllSubterms()) {
            if(!match(term).isPresent()) {
                terms.add(term);
            }
        }
        return factory.makeList(terms);
    }

}
