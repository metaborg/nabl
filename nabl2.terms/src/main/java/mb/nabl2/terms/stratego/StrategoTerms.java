package mb.nabl2.terms.stratego;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.Terms;

public class StrategoTerms {

    public static final String CONS_OP = "str.cons";
    public static final int CONS_ARITY = 2;
    public static final String NIL_OP = "str.nil";
    public static final int NIL_ARITY = 0;

    private final org.spoofax.interpreter.terms.ITermFactory termFactory;

    public StrategoTerms(ITermFactory termFactory) {
        this.termFactory = termFactory;
    }

    // to

    public IStrategoTerm toStratego(ITerm term) {
        // @formatter:off
        IStrategoTerm strategoTerm = term.match(Terms.<IStrategoTerm>cases(
            appl -> {
                final List<IStrategoTerm> args = appl.getArgs().stream().map(arg -> toStratego(arg)).collect(Collectors.toList());
                final int argsSize = args.size();
                IStrategoTerm[] argArray = args.toArray(new IStrategoTerm[argsSize]);
                switch(appl.getOp()) {
                    case Terms.TUPLE_OP:
                        return termFactory.makeTuple(argArray);
                    case CONS_OP:
                        if(argsSize != CONS_ARITY) {
                            throw new IllegalArgumentException("Cons requires two arguments, got " + argsSize);
                        }
                        return termFactory.makeListCons(args.get(0), (IStrategoList) args.get(1));
                    case NIL_OP:
                        if(argsSize != CONS_ARITY) {
                            throw new IllegalArgumentException("Nil requires zero arguments, got " + argsSize);
                        }
                        return termFactory.makeList();
                    default:
                        return termFactory.makeAppl(appl.getOp(), argArray);
                }
            },
            string -> termFactory.makeString(string.getValue()),
            integer -> termFactory.makeInt(integer.getValue()),
            blob -> new StrategoBlob(blob.getValue()),
            var -> termFactory.makeAppl("nabl2.Var", new IStrategoTerm[] { termFactory.makeString(var.getResource()), termFactory.makeString(var.getName()) })
        ));
        // @formatter:on
        switch(strategoTerm.getTermType()) {
            case IStrategoTerm.BLOB:
                break;
            default:
                strategoTerm = putAttachments(strategoTerm, term.getAttachments());
        }
        return strategoTerm;
    }

    private <T extends IStrategoTerm> T putAttachments(T term, ImmutableClassToInstanceMap<Object> attachments) {
        Optional<TermOrigin> origin = TermOrigin.get(attachments);
        if(origin.isPresent()) {
            origin.get().put(term);
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

    public ITerm fromStratego(IStrategoTerm sterm) {
        ImmutableClassToInstanceMap<Object> attachments = getAttachments(sterm);
        // @formatter:off
        final ITerm term = match(sterm, StrategoTerms.<ITerm>cases(
            appl -> {
                final List<ITerm> args = Arrays.asList(appl.getAllSubterms()).stream().map(this::fromStratego).collect(ImmutableList.toImmutableList());
                return B.newAppl(appl.getConstructor().getName(), args, attachments);
            },
            tuple -> {
                final List<ITerm> args = Arrays.asList(tuple.getAllSubterms()).stream().map(this::fromStratego).collect(ImmutableList.toImmutableList());
                return B.newTuple(args, attachments);
            },
            list -> {
                if(list.isEmpty()) {
                    return B.newAppl(NIL_OP, ImmutableList.of(), attachments);
                } else {
                    final ITerm head = fromStratego(list.head());
                    final ITerm tail = fromStratego(list.tail());
                    return B.newAppl(CONS_OP, ImmutableList.of(head, tail), attachments);
                }
            },
            integer -> B.newInt(integer.intValue(), attachments),
            real -> { throw new IllegalArgumentException("Real values are not supported."); },
            string -> B.newString(string.stringValue(), attachments),
            blob -> B.newBlob(blob.value())
        ));
        // @formatter:on
        return term;
    }

    public static ImmutableClassToInstanceMap<Object> getAttachments(IStrategoTerm term) {
        Builder<Object> b = ImmutableClassToInstanceMap.builder();

        TermOrigin.get(term).ifPresent(origin -> {
            b.put(TermOrigin.class, origin);
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
