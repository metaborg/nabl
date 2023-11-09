package mb.nabl2.terms.stratego;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import io.usethesource.capsule.Map;
import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.Pair;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.TermFactory;
import org.spoofax.terms.util.TermUtils;

import jakarta.annotation.Nullable;

/**
 * Works with term indices.
 */
public final class StrategoTermIndices {

    // Prevent instantiation.
    private StrategoTermIndices() {
    }

    private static final String OP = "TermIndex";
    private static final int ARITY = 2;

    // index

    /**
     * Adds term indices to the terms in the given tree,
     * overwriting any existing term indices.
     *
     * @param term the AST to annotate with term indices
     * @param resource the resource identifier, used for the term indices
     * @param termFactory the Stratego term factory to use
     * @return the AST with term indices
     */
    public static IStrategoTerm index(IStrategoTerm term, String resource, ITermFactory termFactory) {
        return new Indexer(resource, termFactory, true, 1, null).index(term);
    }

    /**
     * Adds term indices to the terms in the given tree,
     * keeping any existing term indices.
     *
     * @param term the AST to annotate with term indices
     * @param resource the resource identifier, used for the new term indices
     * @param startIndex the start index for the new term indices; or -1 to start at 1
     * @param termFactory the Stratego term factory to use
     * @return the AST with term indices
     */
    public static IStrategoTerm indexMore(IStrategoTerm term, String resource, int startIndex, ITermFactory termFactory) {
        return new Indexer(resource, termFactory, false, (startIndex >= 1 ? startIndex : 1), null).index(term);
    }

    /**
     * Finds the maximum used term index in the given subtree.
     *
     * @param term the subtree to check
     * @return the maximum term index; or -1 if none were found
     */
    public static int findMaxIndex(final IStrategoTerm term) {
        int maxId = get(term).map(TermIndex::getId).orElse(-1);
        for(IStrategoTerm subterm : term.getSubterms()) {
            maxId = Math.max(maxId, findMaxIndex(subterm));
        }
        return maxId;
    }

    /**
     * Sets indices on the given subtree. Terms that already have a term index will have their term index replaced.
     * This method returns a map with the mappings from the old term indices to the new term indices.
     *
     * @param term the AST to annotate with term indices
     * @param resource the resource name for the new term indices
     * @param startIndex the start index for the new term indices; or -1 to start at 1
     * @param termFactory the Stratego term factory to use
     * @return a tuple with the new term and the mapping from the old term indices to the new term indices
     */
    public static Tuple2<IStrategoTerm, Map.Immutable<TermIndex, TermIndex>> reindex(IStrategoTerm term, String resource, int startIndex, ITermFactory termFactory) {
        final Map.Transient<TermIndex, TermIndex> mapping = Map.Transient.of();
        final IndexerCallback callback = (_term, oldIndex, newIndex) -> {
            if(oldIndex != null) mapping.__put(oldIndex, newIndex);
        };
        final IStrategoTerm newTerm = new Indexer(resource, termFactory, true, (startIndex >= 1 ? startIndex : 1), callback).index(term);
        return Tuple2.of(newTerm, mapping.freeze());
    }

    /**
     * Callback for the indexer.
     */
    private interface IndexerCallback {
        /**
         * Called when a term is given an index.
         *
         * @param term the term that was given an index
         * @param oldIndex the old index of the term, if any; or {@code null} if it had none
         * @param newIndex the new index of the term
         */
        void onIndex(IStrategoTerm term, @Nullable TermIndex oldIndex, TermIndex newIndex);
    }

    /** Adds term indices. */
    private static class Indexer {

        private final String resource;
        private final ITermFactory termFactory;

        private int nextId;
        private boolean overwrite;

        private @Nullable IndexerCallback callback;

        /**
         * Initializes the {@code Indexer} class.
         *
         * @param resource the resource identifier
         * @param termFactory the Stratego term factory to use
         * @param overwrite whether to overwrite/discard any existing term indices
         * @param initialId the term index to start with
         * @param callback callback function; or {@code null}
         */
        public Indexer(String resource, ITermFactory termFactory, boolean overwrite, int initialId, @Nullable IndexerCallback callback) {
            this.resource = resource;
            this.termFactory = termFactory;
            this.overwrite = overwrite;
            this.nextId = initialId;
            this.callback = callback;
        }

        public IStrategoTerm index(final IStrategoTerm term) {
            // Create a copy that we can modify
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

            // Set the term index
            final @Nullable TermIndex oldIndex = get(term).orElse(null);
            if (overwrite || oldIndex == null) {
                // Associate the term's origin, if any, with the new term index
                final TermIndex newIndex = TermIndex.of(resource, nextId++);
                final TermIndex newNewIndex = TermOrigin.get(term).map(o -> o.put(newIndex)).orElse(newIndex);
                result = put(newNewIndex, result, termFactory);
                if (callback != null) callback.onIndex(term, oldIndex, newNewIndex);
            }

            // Copy the original term's attachments to the new term
            termFactory.copyAttachments(term, result);
            return result;
        }

        private IStrategoList index(final IStrategoList list) {
            // Create a copy that we can modify
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
            return terms.stream().map(this::index).toArray(IStrategoTerm[]::new);
        }

    }

    // erase

    /**
     * Removes term indices from the terms in the given tree.
     *
     * @param term        the AST to clear of term indices
     * @param termFactory the Stratego term factory to use
     * @return the AST without term indices
     */
    public static IStrategoTerm erase(IStrategoTerm term, ITermFactory termFactory) {
        return new Eraser(termFactory).erase(term);
    }

    /** Removes term indices. */
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
            return terms.stream().map(this::erase).toArray(IStrategoTerm[]::new);
        }

    }

    // indices of terms

    /**
     * Gets the term index of the given term, if any.
     *
     * @param term the term to check
     * @return the term index of the term, or {@link Optional#empty()} if the term does not have a term index
     */
    public static Optional<TermIndex> get(IStrategoTerm term) {
        for(IStrategoTerm anno : term.getAnnotations()) {
            Optional<TermIndex> index = match(anno);
            if(index.isPresent()) {
                return index;
            }
        }
        return Optional.empty();
    }

    /**
     * Puts a term index on the given term.
     *
     * @param index the term index to add
     * @param term the term to copy
     * @param factory the Stratego term factory to use
     * @return the copied term, with the term index added
     * @param <T> the type of term
     */
    public static <T extends IStrategoTerm> T put(TermIndex index, T term, ITermFactory factory) {
        @SuppressWarnings({ "unchecked" }) T result = (T) factory.annotateTerm(term,
                factory.makeListCons(build(index, factory), removeFromAnnoList(term.getAnnotations(), factory)));
        return result;
    }

    /**
     * Removes a term index from the given term, if the term index is present.
     *
     * @param term the term to copy
     * @param factory the Stratego term factory to use
     * @return the copied term, with the term index removed
     * @param <T>
     */
    public static <T extends IStrategoTerm> T remove(T term, ITermFactory factory) {
        @SuppressWarnings({ "unchecked" }) T result =
                (T) factory.annotateTerm(term, removeFromAnnoList(term.getAnnotations(), factory));
        return result;
    }

    // index terms

    /**
     * Builds a term that represents a term index.
     *
     * @param index the term index to represent
     * @param factory the Stratego term factory to use
     * @return the built term (usually used as an annotation)
     */
    public static IStrategoTerm build(TermIndex index, ITermFactory factory) {
        final IStrategoConstructor ctor = factory.makeConstructor(OP, ARITY);
        final IStrategoTerm indexTerm =
                factory.makeAppl(ctor, factory.makeString(index.getResource()), factory.makeInt(index.getId()));
        TermOrigin.get(index).ifPresent(o -> o.put(indexTerm));
        return indexTerm;
    }

    /**
     * Tests whether the given term is a term index (usually used as an annotation).
     * @param term the term to check
     * @return the term index represented by the given term; or {@link Optional#empty()} if the term is not a term index
     */
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
