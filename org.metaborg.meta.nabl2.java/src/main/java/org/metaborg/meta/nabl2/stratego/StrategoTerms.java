package org.metaborg.meta.nabl2.stratego;

import java.util.List;

import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.metaborg.meta.nabl2.terms.generic.ImmutableTermIndex;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.meta.nabl2.util.functions.Function2;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoPlaceholder;
import org.spoofax.interpreter.terms.IStrategoReal;
import org.spoofax.interpreter.terms.IStrategoRef;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap.Builder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class StrategoTerms {

    private final static String VAR_CTOR = "CVar";
    private final static int VAR_ARITY = 2;

    private final static String LIST_CTOR = "CList";
    private final static String LISTTAIL_CTOR = "CListTail";

    private final Iterable<Function2<IStrategoTerm,Attacher,Unit>> attachmentProviders;

    private final org.spoofax.interpreter.terms.ITermFactory termFactory;
    private IStrategoConstructor varCtor;

    @SafeVarargs public StrategoTerms(ITermFactory termFactory,
            Function2<IStrategoTerm,Attacher,Unit>... attachmentProviders) {
        this.termFactory = termFactory;
        this.varCtor = termFactory.makeConstructor(VAR_CTOR, VAR_ARITY);
        this.attachmentProviders = ImmutableList.copyOf(attachmentProviders);
    }

    public IStrategoTerm toStratego(ITerm term) {
        IStrategoTerm strategoTerm = term.match(Terms.<IStrategoTerm> cases(
                // @formatter:off
                appl -> termFactory.makeAppl(termFactory.makeConstructor(appl.getOp(), appl.getArity()),
                        toStrategos(appl.getArgs()).toArray(new IStrategoTerm[0])),
                list -> termFactory.makeList(toStrategos(list)), string -> termFactory.makeString(string.getValue()),
                integer -> termFactory.makeInt(integer.getValue()), var -> termFactory.makeAppl(varCtor,
                        termFactory.makeString(var.getResource()), termFactory.makeString(var.getName()))
        // @formatter:on
        ));
        putAttachments(strategoTerm, term.getAttachments());
        return strategoTerm;
    }

    public List<IStrategoTerm> toStrategos(Iterable<ITerm> terms) {
        List<IStrategoTerm> strategoTerms = Lists.newArrayList();
        for (ITerm term : terms) {
            strategoTerms.add(toStratego(term));
        }
        return strategoTerms;
    }

    private void putAttachments(IStrategoTerm term, ImmutableClassToInstanceMap<Object> attachments) {
        ImploderAttachment imploderAttachment = attachments.getInstance(ImploderAttachment.class);
        if (imploderAttachment != null) {
            term.putAttachment(imploderAttachment);
        }

        TermIndex termIndex = attachments.getInstance(TermIndex.class);
        if (termIndex != null) {
            StrategoTermIndex.put(term, termIndex.getResource(), termIndex.getId());
        }
    }

    public ITerm fromStratego(IStrategoTerm term) {
        ImmutableClassToInstanceMap<Object> attachments = getAttachments(term);
        ITerm rawTerm = match(term, StrategoTerms.<ITerm> cases(
            // @formatter:off
            appl -> GenericTerms.newAppl(appl.getConstructor().getName(), fromStrategos(appl), attachments),
            tuple -> GenericTerms.newTuple(fromStrategos(tuple), attachments),
            this::fromStrategoList,
            integer -> GenericTerms.newInt(integer.intValue(), attachments),
            real -> { throw new IllegalArgumentException(); },
            string -> GenericTerms.newString(string.stringValue(), attachments),
            ref -> { throw new IllegalArgumentException(); },
            placeholder -> { throw new IllegalArgumentException(); },
            other -> { throw new IllegalArgumentException(); }
            // @formatter:on
        ));
        return M.<ITerm> cases(
            // @formatter:off
            M.appl2(VAR_CTOR, M.stringValue(), M.stringValue(), (v, resource, name) -> GenericTerms.newVar(resource, name)),
            M.appl1(LIST_CTOR, M.list(), (t,xs) -> GenericTerms.newList(xs)),
            M.appl2(LISTTAIL_CTOR, M.list(), M.term(), (t,xs,ys) -> GenericTerms.newListTail(xs, (IListTerm) ys))
            // @formatter:on
        ).match(rawTerm).orElse(rawTerm);
    }

    private IListTerm fromStrategoList(IStrategoList list) {
        ImmutableClassToInstanceMap<Object> attachments = getAttachments(list);
        if (list.isEmpty()) {
            return GenericTerms.newNil(attachments);
        } else {
            return GenericTerms.newCons(fromStratego(list.head()), fromStrategoList(list.tail()), attachments);
        }
    }

    private Iterable<ITerm> fromStrategos(Iterable<IStrategoTerm> strategoTerms) {
        List<ITerm> terms = Lists.newArrayList();
        for (IStrategoTerm strategoTerm : strategoTerms) {
            terms.add(fromStratego(strategoTerm));
        }
        return terms;
    }

    private ImmutableClassToInstanceMap<Object> getAttachments(IStrategoTerm term) {
        Builder<Object> b = ImmutableClassToInstanceMap.builder();

        StrategoTermIndex termIndex = StrategoTermIndex.get(term);
        if (termIndex != null) {
            b.put(TermIndex.class, ImmutableTermIndex.of(termIndex.getResource(), termIndex.getId()));
        }

        ImploderAttachment imploderAttachment = ImploderAttachment.getCompactPositionAttachment(term, false);
        if (imploderAttachment != null) {
            b.put(ImploderAttachment.class, imploderAttachment);
        }

        Attacher attacher = new Attacher() {

            @Override public <T> void put(Class<T> clazz, T instance) {
                b.put(clazz, instance);
            }

        };
        for (Function2<IStrategoTerm,Attacher,Unit> attachmentProvider : attachmentProviders) {
            attachmentProvider.apply(term, attacher);
        }

        return b.build();
    }

    public static <T> T match(IStrategoTerm term, ICases<T> cases) {
        switch (term.getTermType()) {
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
        case IStrategoTerm.REF:
            return cases.caseRef((IStrategoRef) term);
        case IStrategoTerm.PLACEHOLDER:
            return cases.casePlaceholder((IStrategoPlaceholder) term);
        default:
            return cases.otherwise(term);
        }
    }

    public static <T> ICases<T> cases(
            // @formatter:off
            Function1<IStrategoAppl, T> onAppl, Function1<IStrategoTuple, T> onTuple,
            Function1<IStrategoList, T> onList, Function1<IStrategoInt, T> onInt, Function1<IStrategoReal, T> onReal,
            Function1<IStrategoString, T> onString, Function1<IStrategoRef, T> onRef,
            Function1<IStrategoPlaceholder, T> onPlaceholder, Function1<IStrategoTerm, T> otherwise
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

            @Override public T caseRef(IStrategoRef term) {
                return onRef.apply(term);
            }

            @Override public T casePlaceholder(IStrategoPlaceholder term) {
                return onPlaceholder.apply(term);
            }

            @Override public T otherwise(IStrategoTerm term) {
                return otherwise.apply(term);
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

        public T caseRef(IStrategoRef term);

        public T casePlaceholder(IStrategoPlaceholder term);

        public T otherwise(IStrategoTerm term);

    }

    @FunctionalInterface
    public interface Attacher {

        <T> void put(Class<T> clazz, T instance);

    }

}