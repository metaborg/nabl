package mb.nabl2.terms.stratego;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Function1;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoPlaceholder;
import org.spoofax.interpreter.terms.IStrategoReal;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.StrategoPlaceholder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.IBlobTerm;
import mb.nabl2.terms.IConsTerm;
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.build.Attachments;
import mb.nabl2.terms.matching.VarProvider;

import static mb.nabl2.terms.build.TermBuild.B;

public class StrategoTerms {

    private final org.spoofax.interpreter.terms.ITermFactory termFactory;

    public StrategoTerms(ITermFactory termFactory) {
        this.termFactory = termFactory;
    }

    // to

    public IStrategoTerm toStratego(ITerm term) {
        return toStratego(term, false);
    }

    public IStrategoTerm toStratego(ITerm term, boolean varsToPlhdrs) {
        IStrategoTerm strategoTerm = null;
        switch(term.termTag()) {
            case IApplTerm: {
                IApplTerm appl = (IApplTerm) term;
                List<ITerm> args = appl.getArgs();
                IStrategoTerm[] argArray = appl.getArgs().stream().map(arg -> toStratego(arg, varsToPlhdrs)).toArray(IStrategoTerm[]::new);
                strategoTerm = appl.getOp().equals(Terms.TUPLE_OP) ? termFactory.makeTuple(argArray) :
                    termFactory.makeAppl(termFactory.makeConstructor(appl.getOp(), appl.getArity()), argArray);
                break;
            }

            case IConsTerm:
            case INilTerm: {
                IListTerm list = (IListTerm) term;
                strategoTerm = toStrategoList(list, varsToPlhdrs);
                break;
            }

            case IStringTerm: {
                IStringTerm string = (IStringTerm) term;
                strategoTerm = termFactory.makeString(string.getValue());
                break;
            }

            case IIntTerm: {
                IIntTerm integer = (IIntTerm) term;
                strategoTerm = termFactory.makeInt(integer.getValue());
                break;
            }

            case IBlobTerm: {
                IBlobTerm blob = (IBlobTerm) term;
                strategoTerm = new StrategoBlob(blob.getValue());
                break;
            }

            case ITermVar: {
                ITermVar var = (ITermVar) term;
                if(varsToPlhdrs) {
                    strategoTerm = termFactory.makePlaceholder(termFactory.makeTuple(termFactory.makeString(var.getResource()),
                        termFactory.makeString(var.getName())));
                } else {
                    strategoTerm = termFactory.makeAppl("nabl2.Var", termFactory.makeString(var.getResource()),
                        termFactory.makeString(var.getName()));
                }
                break;
            }
        }
        assert strategoTerm != null;
        switch(strategoTerm.getType()) {
            case BLOB:
            case LIST:
                break;
            default:
                strategoTerm = putAttachments(strategoTerm, term.getAttachments());
        }
        return strategoTerm;
    }

    private IStrategoTerm toStrategoList(IListTerm list, boolean varsToPlhdrs) {
        final Deque<IStrategoTerm> terms = new ArrayDeque<>(list.getMinSize());
        final Deque<IAttachments> attachments = new ArrayDeque<>(list.getMinSize());
        while(list != null) {
            attachments.push(list.getAttachments());
            switch(list.listTermTag()) {
                case IConsTerm: {
                    IConsTerm cons = (IConsTerm) list;
                    terms.push(toStratego(cons.getHead(), varsToPlhdrs));
                    list = cons.getTail();
                    break;
                }

                case INilTerm: {
                    list = null;
                    break;
                }

                case ITermVar: {
                    throw new IllegalArgumentException(
                        "Cannot convert specialized terms to Stratego.");
                }
            }
        }
        IStrategoList strategoList = termFactory.makeList();
        putAttachments(strategoList, attachments.pop());
        while(!terms.isEmpty()) {
            strategoList = termFactory.makeListCons(terms.pop(), strategoList);
            putAttachments(strategoList, attachments.pop());
        }
        return strategoList;
    }

    private <T extends IStrategoTerm> T putAttachments(T term, IAttachments attachments) {
        if(attachments.isEmpty()) {
            return term;
        }

        Optional<TermOrigin> origin = TermOrigin.get(attachments);
        if(origin.isPresent()) {
            origin.get().put(term);
        }

        Optional<TermIndex> index = TermIndex.get(attachments);
        if(index.isPresent()) {
            term = StrategoTermIndices.put(index.get(), term, termFactory);
        }

        @Nullable StrategoAnnotations annotations = attachments.get(StrategoAnnotations.class);
        if(annotations != null && !annotations.isEmpty()) {
            @SuppressWarnings({ "unchecked" }) T result = (T) termFactory.copyAttachments(term,
                    termFactory.annotateTerm(term, termFactory.makeList(annotations.getAnnotationList())));
            term = result;
        }

        return term;
    }

    // from

    public ITerm fromStratego(IStrategoTerm sterm) {
        return fromStratego(sterm, null);
    }

    public ITerm fromStratego(IStrategoTerm term, @Nullable VarProvider varProvider) {
        @Nullable IAttachments attachments = getAttachments(term);
        ITerm result;
        switch(term.getType()) {
            case APPL: {
                IStrategoAppl appl = (IStrategoAppl) term;
                final IStrategoTerm[] subTerms = appl.getAllSubterms();
                final ImmutableList.Builder<ITerm> args = ImmutableList.builderWithExpectedSize(subTerms.length);
                for(IStrategoTerm subTerm : subTerms) {
                    args.add(fromStratego(subTerm, varProvider));
                }
                result = B.newAppl(appl.getConstructor().getName(), args.build(), attachments);
                break;
            }
            case TUPLE: {
                IStrategoTuple tuple = (IStrategoTuple) term;
                final IStrategoTerm[] subTerms = tuple.getAllSubterms();
                final ImmutableList.Builder<ITerm> args = ImmutableList.builderWithExpectedSize(subTerms.length);
                for(IStrategoTerm subTerm : subTerms) {
                    args.add(fromStratego(subTerm, varProvider));
                }
                result = B.newTuple(args.build(), attachments);
                break;
            }
            case LIST: {
                result = fromStrategoList((IStrategoList) term, varProvider);
                break;
            }
            case INT: {
                IStrategoInt integer = (IStrategoInt) term;
                result = B.newInt(integer.intValue(), attachments);
                break;
            }
            case REAL: {
                throw new IllegalArgumentException("Real values are not supported.");
            }
            case STRING: {
                IStrategoString string = (IStrategoString) term;
                result = B.newString(string.stringValue(), attachments);
                break;
            }
            case BLOB: {
                StrategoBlob blob = (StrategoBlob) term;
                result = B.newBlob(blob.value());
                break;
            }
            case PLACEHOLDER: {
                if(varProvider != null) {
                    result = varProvider.freshWld();
                } else {
                    throw new IllegalArgumentException("Placeholders are not supported.");
                }
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported Stratego term type " + term.getType());
            }
        }
        return result;
    }

    private IListTerm fromStrategoList(IStrategoList list, @Nullable VarProvider varProvider) {
        final LinkedList<ITerm> terms = Lists.newLinkedList();
        final LinkedList<IAttachments> attachments = Lists.newLinkedList();
        while(!list.isEmpty()) {
            terms.add(fromStratego(list.head(), varProvider));
            attachments.push(getAttachments(list));
            list = list.tail();
        }
        attachments.add(getAttachments(list));
        return B.newList(terms, attachments);
    }

    public static IAttachments getAttachments(IStrategoTerm term) {
        final Attachments.Builder b = Attachments.Builder.of();

        TermOrigin.get(term).ifPresent(origin -> b.put(TermOrigin.class, origin));

        StrategoTermIndices.get(term).ifPresent(termIndex -> b.put(TermIndex.class, termIndex));

        final IStrategoList annos = term.getAnnotations();
        if(!annos.isEmpty()) {
            b.put(StrategoAnnotations.class, StrategoAnnotations.of(annos));
        }

        return b.build();
    }

    // matching

    public static <T> T match(IStrategoTerm term, ICases<T> cases) {
        switch(term.getType()) {
            case APPL:
                return cases.caseAppl((IStrategoAppl) term);
            case LIST:
                return cases.caseList((IStrategoList) term);
            case TUPLE:
                return cases.caseTuple((IStrategoTuple) term);
            case INT:
                return cases.caseInt((IStrategoInt) term);
            case REAL:
                return cases.caseReal((IStrategoReal) term);
            case STRING:
                return cases.caseString((IStrategoString) term);
            case BLOB:
                if(term instanceof StrategoBlob) {
                    StrategoBlob blob = (StrategoBlob) term;
                    return cases.caseBlob(blob);
                } else {
                    throw new IllegalArgumentException("Unsupported Stratego blob type " + term.getClass());
                }
            case PLACEHOLDER:
                return cases.casePlhdr((StrategoPlaceholder) term);
            default:
                throw new IllegalArgumentException("Unsupported Stratego term type " + term.getType());
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
        Function1<StrategoBlob, T> onBlob,
        Function1<IStrategoPlaceholder, T> onPlhdr
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

            @Override public T casePlhdr(IStrategoPlaceholder term) {
                return onPlhdr.apply(term);
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

        public T casePlhdr(IStrategoPlaceholder term);

    }

    @FunctionalInterface
    public interface Attacher {

        <T> void put(Class<T> clazz, T instance);

    }

}
