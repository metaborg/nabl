package org.metaborg.meta.nabl2.stratego;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ListTerms;
import org.metaborg.meta.nabl2.terms.Terms;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoReal;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.ITermFactory;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap.Builder;

public class StrategoTerms {

    private final static String VAR_CTOR = "CVar";
    private final static int VAR_ARITY = 2;

    private final static String LIST_CTOR = "CList";
    private final static String LISTTAIL_CTOR = "CListTail";

    private final org.spoofax.interpreter.terms.ITermFactory termFactory;
    private final IStrategoConstructor varCtor;

    public StrategoTerms(ITermFactory termFactory) {
        this.termFactory = termFactory;
        this.varCtor = termFactory.makeConstructor(VAR_CTOR, VAR_ARITY);
    }

    public IStrategoTerm toStratego(ITerm term) {
        IStrategoTerm strategoTerm = term.match(Terms.cases(
            // @formatter:off
            appl -> {
                List<IStrategoTerm> args = appl.getArgs().stream().map(this::toStratego).collect(Collectors.toList());
                IStrategoTerm[] argArray = args.toArray(new IStrategoTerm[args.size()]);
                return appl.getOp().equals(Terms.TUPLE_OP)
                        ? termFactory.makeTuple(argArray)
                        : termFactory.makeAppl(termFactory.makeConstructor(appl.getOp(), appl.getArity()), argArray);
            },
            list -> toStrategoList(list),
            string -> termFactory.makeString(string.getValue()),
            integer -> termFactory.makeInt(integer.getValue()),
            var -> termFactory.makeAppl(varCtor, termFactory.makeString(var.getResource()), termFactory.makeString(var.getName()))
            // @formatter:on
        ));
        return putAttachments(strategoTerm, term.getAttachments());
    }

    public IStrategoList toStrategoList(IListTerm list) {
        IStrategoList strategoList = list.match(ListTerms.cases(
            // @formatter:off
            cons -> termFactory.makeListCons(toStratego(cons.getHead()), toStrategoList(cons.getTail())),
            nil -> termFactory.makeList(),
            var -> { throw new IllegalArgumentException(); }
            // @formatter:on
        ));
        return putAttachments(strategoList, list.getAttachments());
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

    public ITerm fromStratego(IStrategoTerm term) {
        ImmutableClassToInstanceMap<Object> attachments = getAttachments(term);
        ITerm rawTerm = match(term,
                StrategoTerms.<ITerm>cases(
            // @formatter:off
            appl -> GenericTerms.newAppl(appl.getConstructor().getName(), Arrays.asList(appl.getAllSubterms()).stream().map(this::fromStratego).collect(Collectors.toList())),
            tuple -> GenericTerms.newTuple(Arrays.asList(tuple.getAllSubterms()).stream().map(this::fromStratego).collect(Collectors.toList())),
            this::fromStrategoList,
            integer -> GenericTerms.newInt(integer.intValue()),
            real -> { throw new IllegalArgumentException(); },
            string -> GenericTerms.newString(string.stringValue())
            // @formatter:on
                )).withAttachments(attachments);
        return M.<ITerm>cases(
            // @formatter:off
            M.appl2(VAR_CTOR, M.stringValue(), M.stringValue(), (v, resource, name) ->
                    GenericTerms.newVar(resource, name).withAttachments(v.getAttachments())),
            M.appl1(LIST_CTOR, M.list(), (t,xs) -> GenericTerms.newList(xs).withAttachments(t.getAttachments())),
            M.appl2(LISTTAIL_CTOR, M.list(), M.term(), (t,xs,ys) ->
                    GenericTerms.newListTail(xs, (IListTerm) ys).withAttachments(t.getAttachments()))
            // @formatter:on
        ).match(rawTerm).orElse(rawTerm);
    }

    private IListTerm fromStrategoList(IStrategoList list) {
        Deque<ITerm> terms = new ArrayDeque<>();
        Deque<ImmutableClassToInstanceMap<Object>> attachments = new ArrayDeque<>();
        while(!list.isEmpty()) {
            terms.push(fromStratego(list.head()));
            attachments.push(getAttachments(list));
            list = list.tail();
        }
        IListTerm newList = GenericTerms.newNil(getAttachments(list));
        while(!terms.isEmpty()) {
            newList = GenericTerms.newCons(terms.pop(), newList, attachments.pop());
        }
        return newList;
    }

    private ImmutableClassToInstanceMap<Object> getAttachments(IStrategoTerm term) {
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
        Function1<IStrategoString, T> onString
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

        };
    }

    public interface ICases<T> {

        public T caseAppl(IStrategoAppl term);

        public T caseList(IStrategoList term);

        public T caseTuple(IStrategoTuple term);

        public T caseInt(IStrategoInt term);

        public T caseReal(IStrategoReal term);

        public T caseString(IStrategoString term);

    }

    @FunctionalInterface
    public interface Attacher {

        <T> void put(Class<T> clazz, T instance);

    }

}