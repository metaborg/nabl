package mb.nabl2.terms.stratego;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoPlaceholder;
import org.spoofax.interpreter.terms.IStrategoReal;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.StrategoTerm;
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
            IStrategoTerm result;
            switch(term.getType()) {
                case APPL: {
                    IStrategoAppl appl = (IStrategoAppl) term;
                    result =
                        termFactory.makeAppl(appl.getConstructor(), index(appl.getAllSubterms()),
                            appl.getAnnotations());
                    break;
                }
                case TUPLE: {
                    IStrategoTuple tuple = (IStrategoTuple) term;
                    result = termFactory.makeTuple(index(tuple.getAllSubterms()),
                        tuple.getAnnotations());
                    break;
                }
                case LIST: {
                    result = index((IStrategoList) term);
                    break;
                }
                case INT: {
                    IStrategoInt integer = (IStrategoInt) term;
                    result = termFactory.annotateTerm(termFactory.makeInt(integer.intValue()),
                        integer.getAnnotations());
                    break;
                }
                case REAL: {
                    IStrategoReal real = (IStrategoReal) term;
                    result = termFactory.annotateTerm(termFactory.makeReal(real.realValue()),
                        real.getAnnotations());
                    break;
                }
                case STRING: {
                    IStrategoString string = (IStrategoString) term;
                    result = termFactory.annotateTerm(termFactory.makeString(string.stringValue()),
                        string.getAnnotations());
                    break;
                }
                case BLOB: {
                    StrategoBlob blob = (StrategoBlob) term;
                    result = new StrategoBlob(blob.value());
                    break;
                }
                case PLACEHOLDER: {
                    IStrategoPlaceholder plhdr = (IStrategoPlaceholder) term;
                    result =
                        termFactory.annotateTerm(termFactory.makePlaceholder(plhdr.getTemplate()),
                            plhdr.getAnnotations());
                    break;
                }
                default: {
                    throw new IllegalArgumentException(
                        "Unsupported Stratego term type " + term.getType());
                }
            }
            final TermIndex index1 = TermIndex.of(resource, ++currentId);
            final TermIndex index2 = TermOrigin.get(term).map(o -> o.put(index1)).orElse(index1);
            result = put(index2, result, termFactory);
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
            termFactory.copyAttachments(list, result);
            final TermIndex index1 = TermIndex.of(resource, ++currentId);
            final TermIndex index2 = TermOrigin.get(list).map(o -> o.put(index1)).orElse(index1);
            result = put(index2, result, termFactory);
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
            IStrategoTerm result;
            switch(term.getType()) {
                case APPL: {
                    IStrategoAppl appl = (IStrategoAppl) term;
                    result =
                        termFactory.makeAppl(appl.getConstructor(), erase(appl.getAllSubterms()),
                            appl.getAnnotations());
                    break;
                }
                case TUPLE: {
                    IStrategoTuple tuple = (IStrategoTuple) term;
                    result = termFactory.makeTuple(erase(tuple.getAllSubterms()),
                        tuple.getAnnotations());
                    break;
                }
                case LIST: {
                    result = erase((IStrategoList) term);
                    break;
                }
                case INT: {
                    IStrategoInt integer = (IStrategoInt) term;
                    result = termFactory.annotateTerm(termFactory.makeInt(integer.intValue()),
                        integer.getAnnotations());
                    break;
                }
                case REAL: {
                    IStrategoReal real = (IStrategoReal) term;
                    result = termFactory.annotateTerm(termFactory.makeReal(real.realValue()),
                        real.getAnnotations());
                    break;
                }
                case STRING: {
                    IStrategoString string = (IStrategoString) term;
                    result = termFactory.annotateTerm(termFactory.makeString(string.stringValue()),
                        string.getAnnotations());
                    break;
                }
                case BLOB: {
                    StrategoBlob blob = (StrategoBlob) term;
                    result = new StrategoBlob(blob.value());
                    break;
                }
                case PLACEHOLDER: {
                    IStrategoPlaceholder plhdr = (IStrategoPlaceholder) term;
                    result =
                        termFactory.annotateTerm(termFactory.makePlaceholder(plhdr.getTemplate()),
                            plhdr.getAnnotations());
                    break;
                }
                default: {
                    throw new IllegalArgumentException(
                        "Unsupported Stratego term type " + term.getType());
                }
            }
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
            result = remove(result, termFactory);
            assert !get(result).isPresent();
            return result;
        }

        private IStrategoTerm[] erase(final IStrategoTerm[] terms) {
            return Arrays.asList(terms).stream().map(this::erase).toArray(n -> new IStrategoTerm[n]);
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
        final TermIndex index2 = TermOrigin.get(term).map(o -> o.put(index1)).orElse(index1);
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
