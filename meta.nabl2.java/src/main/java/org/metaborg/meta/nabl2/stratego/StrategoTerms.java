package org.metaborg.meta.nabl2.stratego;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ListTerms;
import org.metaborg.meta.nabl2.terms.Terms;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.util.functions.Function1;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoReal;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.ITermFactory;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap.Builder;
import com.google.common.collect.Lists;

public class StrategoTerms {

    private final org.spoofax.interpreter.terms.ITermFactory termFactory;

    public StrategoTerms(ITermFactory termFactory) {
        this.termFactory = termFactory;
    }

    // to

    public IStrategoTerm toStratego(ITerm term) {
        IStrategoTerm strategoTerm = term.match(Terms.cases(
            // @formatter:off
            appl -> {
                List<IStrategoTerm> args = appl.getArgs().stream().map(arg -> toStratego(arg)).collect(Collectors.toList());
                IStrategoTerm[] argArray = args.toArray(new IStrategoTerm[args.size()]);
                return appl.getOp().equals(Terms.TUPLE_OP)
                        ? termFactory.makeTuple(argArray)
                        : termFactory.makeAppl(termFactory.makeConstructor(appl.getOp(), appl.getArity()), argArray);
            },
            list ->  toStrategoList(list),
            string -> termFactory.makeString(string.getValue()),
            integer -> termFactory.makeInt(integer.getValue()),
            blob -> new StrategoBlob(blob.getValue()),
            var -> {
                throw new IllegalArgumentException("Cannot convert specialized terms to Stratego.");
            }
            // @formatter:on
        ));
        strategoTerm = putAttachments(strategoTerm, term.getAttachments());
        return strategoTerm;
    }

    // NB. This function does not preserve locks, it depends on toStratego for that.
    private IStrategoTerm toStrategoList(IListTerm list) {
        final LinkedList<IStrategoTerm> terms = Lists.newLinkedList();
        final LinkedList<ImmutableClassToInstanceMap<Object>> attachments = Lists.newLinkedList();
        while(list != null) {
            attachments.push(list.getAttachments());
            list = list.match(ListTerms.<IListTerm>cases(
                // @formatter:off
                cons -> {
                    terms.push(toStratego(cons.getHead()));
                    return cons.getTail();
                },
                nil -> {
                    return null;
                },
                var -> {
                    throw new IllegalArgumentException("Cannot convert specialized terms to Stratego.");
                }
                // @formatter:on
            ));
        }
        IStrategoList strategoList = termFactory.makeList();
        putAttachments(strategoList, attachments.pop());
        while(!terms.isEmpty()) {
            strategoList = termFactory.makeListCons(terms.pop(), strategoList);
            putAttachments(strategoList, attachments.pop());
        }
        return strategoList;
    }

    private <T extends IStrategoTerm> T putAttachments(T term, ImmutableClassToInstanceMap<Object> attachments) {
        Optional<TermOrigin> origin = TermOrigin.get(attachments);
        if(origin.isPresent()) {
            term.putAttachment(origin.get().toImploderAttachment());
        }

        Optional<TermIndex> index = TermIndex.get(attachments);
        if(index.isPresent()) {
            term = StrategoTermIndices.put(index.get(), term, termFactory);
        }

        StrategoAnnotations annotations = attachments.getInstance(StrategoAnnotations.class);
        if(annotations != null) {
            @SuppressWarnings({ "unchecked" }) T result = (T) termFactory.copyAttachments(term,
                    termFactory.annotateTerm(term, termFactory.makeList(annotations.getAnnotationList())));
            term = result;
        }

        return term;
    }

    // from

    public static ITerm fromStratego(IStrategoTerm sterm) {
        ImmutableClassToInstanceMap<Object> attachments = getAttachments(sterm);
        // @formatter:off
        ITerm term = match(sterm, StrategoTerms.cases(
            appl -> TB.newAppl(appl.getConstructor().getName(), Arrays.asList(appl.getAllSubterms()).stream().map(StrategoTerms::fromStratego).collect(Collectors.toList())),
            tuple -> TB.newTuple(Arrays.asList(tuple.getAllSubterms()).stream().map(StrategoTerms::fromStratego).collect(Collectors.toList())),
            StrategoTerms::fromStrategoList,
            integer -> TB.newInt(integer.intValue()),
            real -> { throw new IllegalArgumentException("Real values are not supported."); },
            string -> TB.newString(string.stringValue()),
            blob -> TB.newBlob(blob.value())
        )).withAttachments(attachments);
        // @formatter:on
        return term;
    }

    private static IListTerm fromStrategoList(IStrategoList list) {
        final LinkedList<ITerm> terms = Lists.newLinkedList();
        final LinkedList<ImmutableClassToInstanceMap<Object>> attachments = Lists.newLinkedList();
        while(!list.isEmpty()) {
            terms.add(fromStratego(list.head()));
            attachments.push(getAttachments(list));
            list = list.tail();
        }
        attachments.add(getAttachments(list));
        return TB.newList(terms, attachments);
    }

    private static ImmutableClassToInstanceMap<Object> getAttachments(IStrategoTerm term) {
        Builder<Object> b = ImmutableClassToInstanceMap.builder();

        TermOrigin.getImploderAttachment(term).ifPresent(imploderAttachment -> {
            b.put(TermOrigin.class, TermOrigin.fromImploderAttachment(imploderAttachment));
        });

        StrategoTermIndices.get(term).ifPresent(termIndex -> {
            b.put(TermIndex.class, termIndex);
        });

        b.put(StrategoAnnotations.class, ImmutableStrategoAnnotations.of(term.getAnnotations()));

        return b.build();
    }

    // matching

    public static <T> T match(IStrategoTerm term, ICases<T> cases) {
        switch(term.getTermType()) {
            case IStrategoTerm.APPL:
                return cases.caseAppl((IStrategoAppl) term);
            case IStrategoTerm.LIST:
                return cases.caseList((IStrategoList) term);
            case IStrategoTerm.TUPLE:
                return cases.caseTuple((IStrategoTuple) term);
            case IStrategoTerm.INT:
                return cases.caseInt((IStrategoInt) term);
            case IStrategoTerm.REAL:
                return cases.caseReal((IStrategoReal) term);
            case IStrategoTerm.STRING:
                return cases.caseString((IStrategoString) term);
            case IStrategoTerm.BLOB:
                if(term instanceof StrategoBlob) {
                    StrategoBlob blob = (StrategoBlob) term;
                    return cases.caseBlob(blob);
                } else {
                    throw new IllegalArgumentException("Unsupported Stratego blob type " + term.getClass());
                }
            default:
                throw new IllegalArgumentException("Unsupported Stratego term type " + term.getTermType());
        }
    }

    public static <T> ICases<T> cases(
        // @formatter:off
        Function1<IStrategoAppl, T> onAppl,
        Function1<IStrategoTuple, T> onTuple,
        Function1<IStrategoList, T> onList,
        Function1<IStrategoInt, T> onInt,
        Function1<IStrategoReal, T> onReal,
        Function1<IStrategoString, T> onString,
        Function1<StrategoBlob, T> onBlob
        // @formatter:on
    ) {
        return new ICases<T>() {

            @Override public T caseAppl(IStrategoAppl term) {
                return onAppl.apply(term);
            }

            @Override public T caseTuple(IStrategoTuple term) {
                return onTuple.apply(term);
            }

            @Override public T caseList(IStrategoList term) {
                return onList.apply(term);
            }

            @Override public T caseInt(IStrategoInt term) {
                return onInt.apply(term);
            }

            @Override public T caseReal(IStrategoReal term) {
                return onReal.apply(term);
            }

            @Override public T caseString(IStrategoString term) {
                return onString.apply(term);
            }

            @Override public T caseBlob(StrategoBlob term) {
                return onBlob.apply(term);
            }

        };
    }

    public interface ICases<T> {

        public T caseAppl(IStrategoAppl term);

        public T caseList(IStrategoList term);

        public T caseTuple(IStrategoTuple term);

        public T caseInt(IStrategoInt term);

        public T caseReal(IStrategoReal term);

        public T caseString(IStrategoString term);

        public T caseBlob(StrategoBlob term);

    }

    @FunctionalInterface
    public interface Attacher {

        <T> void put(Class<T> clazz, T instance);

    }

}