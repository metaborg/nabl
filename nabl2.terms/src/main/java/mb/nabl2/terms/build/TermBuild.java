package mb.nabl2.terms.build;

import javax.annotation.Nullable;

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
            if((attachments == null || attachments.isEmpty())) {
                final IApplTerm term = ApplTerm.of(op, args);
                return term.getArity() == 0 ? (IApplTerm) cache.getOrPut(term, term) : term;
            } else {
                return ApplTerm.builder().op(op).args(args).attachments(attachments).build();
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