package mb.nabl2.terms.build;

import java.util.WeakHashMap;

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

public class TermBuild {

    public static final B B = new B();

    public static class B implements ITermBuild {

        private static final WeakHashMap<ITerm, ITerm> cache = new WeakHashMap<>();

        @Override public IApplTerm newAppl(String op, Iterable<? extends ITerm> args,
                @Nullable IAttachments attachments) {
            final IApplTerm term = ApplTerm.of(op, args);
            if((attachments == null || attachments.isEmpty())) {
                return (IApplTerm) cache.computeIfAbsent(term, t -> term);
            } else {
                return term.withAttachments(attachments);
            }
        }

        @Override public INilTerm newNil(@Nullable IAttachments attachments) {
            final INilTerm term = NilTerm.of();
            if((attachments == null || attachments.isEmpty())) {
                return (INilTerm) cache.computeIfAbsent(term, t -> term);
            } else {
                return term.withAttachments(attachments);
            }
        }

        @Override public IConsTerm newCons(ITerm head, IListTerm tail, @Nullable IAttachments attachments) {
            final IConsTerm term = ConsTerm.of(head, tail);
            if((attachments == null || attachments.isEmpty())) {
                return (IConsTerm) cache.computeIfAbsent(term, t -> term);
            } else {
                return term.withAttachments(attachments);
            }
        }

        @Override public IStringTerm newString(String value, @Nullable IAttachments attachments) {
            final IStringTerm term = StringTerm.of(value);
            if((attachments == null || attachments.isEmpty())) {
                return (IStringTerm) cache.computeIfAbsent(term, t -> term);
            } else {
                return term.withAttachments(attachments);
            }
        }

        @Override public IIntTerm newInt(int value, @Nullable IAttachments attachments) {
            final IIntTerm term = IntTerm.of(value);
            if((attachments == null || attachments.isEmpty())) {
                return (IIntTerm) cache.computeIfAbsent(term, t -> term);
            } else {
                return term.withAttachments(attachments);
            }
        }

        @Override public IBlobTerm newBlob(Object value, @Nullable IAttachments attachments) {
            final IBlobTerm term = BlobTerm.of(value);
            if((attachments == null || attachments.isEmpty())) {
                return (IBlobTerm) cache.computeIfAbsent(term, t -> term);
            } else {
                return term.withAttachments(attachments);
            }
        }

        @Override public ITermVar newVar(String resource, String name, @Nullable IAttachments attachments) {
            final ITermVar term = TermVar.of(resource, name);
            if((attachments == null || attachments.isEmpty())) {
                return (ITermVar) cache.computeIfAbsent(term, t -> term);
            } else {
                return term.withAttachments(attachments);
            }
        }

    }

}