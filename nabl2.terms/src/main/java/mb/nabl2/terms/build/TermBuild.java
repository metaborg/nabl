package mb.nabl2.terms.build;

import java.util.List;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.ImList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.IBlobTerm;
import mb.nabl2.terms.IConsTerm;
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.INilTerm;
import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.collections.ConcurrentWeakCache;

public class TermBuild {

    public static final B B = new B();

    public static class B implements ITermBuild {

        private static final INilTerm NIL = NilTerm.builder().build();

        // FIXME Use hash-consing to improve sharing between simple terms. Terms could be shared if:
        // 1. They have no attachments.
        // 2. The have no subterms (because their subterms may have attachments, even if the outer term does not).
        // In practice this means mostly nil, strings, ints, and variables are shared.
        final ConcurrentWeakCache<ITerm, ITerm> cache = new ConcurrentWeakCache<>();

        @Override public IApplTerm newAppl(String op, Iterable<? extends ITerm> args,
                @Nullable IAttachments attachments) {
            final ImList.Immutable<ITerm> argList = ImList.Immutable.copyOf(args);
            switch(argList.size()) {
                case 0: {
                    if((attachments == null || attachments.isEmpty())) {
                        final ITerm term = Appl0Term.of(op);
                        return (IApplTerm) cache.getOrPut(term, term);
                    } else {
                        return Appl0Term.builder().op(op).attachments(attachments).build();
                    }
                }
                case 1: {
                    if((attachments == null || attachments.isEmpty())) {
                        return Appl1Term.of(op, argList.get(0));
                    } else {
                        return Appl1Term.builder().op(op).arg0(argList.get(0)).attachments(attachments).build();
                    }
                }
                case 2: {
                    if((attachments == null || attachments.isEmpty())) {
                        return Appl2Term.of(op, argList.get(0), argList.get(1));
                    } else {
                        return Appl2Term.builder().op(op).arg0(argList.get(0)).arg1(argList.get(1))
                                .attachments(attachments).build();
                    }
                }
                case 3: {
                    if((attachments == null || attachments.isEmpty())) {
                        return Appl3Term.of(op, argList.get(0), argList.get(1), argList.get(2));
                    } else {
                        return Appl3Term.builder().op(op).arg0(argList.get(0)).arg1(argList.get(1)).arg2(argList.get(2))
                                .attachments(attachments).build();
                    }
                }
                case 4: {
                    if((attachments == null || attachments.isEmpty())) {
                        return Appl4Term.of(op, argList.get(0), argList.get(1), argList.get(2), argList.get(3));
                    } else {
                        return Appl4Term.builder().op(op).arg0(argList.get(0)).arg1(argList.get(1)).arg2(argList.get(2))
                                .arg3(argList.get(3)).attachments(attachments).build();
                    }
                }
                default: {
                    if((attachments == null || attachments.isEmpty())) {
                        return ApplTerm.of(op, argList);
                    } else {
                        return ApplTerm.builder().op(op).args(argList).attachments(attachments).build();
                    }
                }
            }
        }

        @Override public INilTerm newNil(@Nullable IAttachments attachments) {
            if((attachments == null || attachments.isEmpty())) {
                return NIL;
            } else {
                return NilTerm.builder().attachments(attachments).build();
            }
        }

        @Override public IConsTerm newCons(ITerm head, IListTerm tail, @Nullable IAttachments attachments) {
            if(attachments == null || attachments.isEmpty()) {
                return ConsTerm.of(head, tail);
            } else {
                return ConsTerm.builder().head(head).tail(tail).attachments(attachments).build();
            }
        }

        @Override public IStringTerm newString(String value, @Nullable IAttachments attachments) {
            if((attachments == null || attachments.isEmpty())) {
                final IStringTerm term = StringTerm.of(value);
                return (IStringTerm) cache.getOrPut(term, term);
            } else {
                return StringTerm.builder().value(value).attachments(attachments).build();
            }
        }

        @Override public IIntTerm newInt(int value, @Nullable IAttachments attachments) {
            if((attachments == null || attachments.isEmpty())) {
                final IIntTerm term = IntTerm.of(value);
                return (IIntTerm) cache.getOrPut(term, term);
            } else {
                return IntTerm.builder().value(value).attachments(attachments).build();
            }
        }

        @Override public IBlobTerm newBlob(Object value, @Nullable IAttachments attachments) {
            if(attachments == null || attachments.isEmpty()) {
                return BlobTerm.of(value);
            } else {
                return BlobTerm.builder().value(value).attachments(attachments).build();
            }
        }

        @Override public ITermVar newVar(String resource, String name, @Nullable IAttachments attachments) {
            if((attachments == null || attachments.isEmpty())) {
                final ITermVar term = TermVar.of(resource, name);
                return (ITermVar) cache.getOrPut(term, term);
            } else {
                return TermVar.builder().resource(resource).name(name).attachments(attachments).build();
            }
        }

    }

}