package mb.nabl2.terms.build;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

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
import mb.nabl2.terms.Terms;

public class TermBuild {

    public static final B B = new B();

    public static class B implements ITermBuild {

        public final IApplTerm EMPTY_TUPLE = newAppl(Terms.TUPLE_OP, ImmutableList.of());

        public final INilTerm EMPTY_LIST = newNil();

        @Override public IApplTerm newAppl(String op, Iterable<? extends ITerm> args,
                @Nullable IAttachments attachments) {
            final IApplTerm term = ApplTerm.of(op, args);
            return attachments != null ? term.withAttachments(attachments) : term;
        }

        @Override public INilTerm newNil(@Nullable IAttachments attachments) {
            final INilTerm term = EMPTY_LIST;
            return attachments != null ? term.withAttachments(attachments) : term;
        }

        @Override public IConsTerm newCons(ITerm head, IListTerm tail,
                @Nullable IAttachments attachments) {
            final IConsTerm term = ConsTerm.of(head, tail);
            return attachments != null ? term.withAttachments(attachments) : term;
        }

        @Override public IStringTerm newString(String value,
                @Nullable IAttachments attachments) {
            final IStringTerm term = StringTerm.of(value);
            return attachments != null ? term.withAttachments(attachments) : term;
        }

        @Override public IIntTerm newInt(int value, @Nullable IAttachments attachments) {
            final IIntTerm term = IntTerm.of(value);
            return attachments != null ? term.withAttachments(attachments) : term;
        }


        @Override public IBlobTerm newBlob(Object value, @Nullable IAttachments attachments) {
            final IBlobTerm term = BlobTerm.of(value);
            return attachments != null ? term.withAttachments(attachments) : term;
        }

        @Override public ITermVar newVar(String resource, String name,
                @Nullable IAttachments attachments) {
            final ITermVar term = TermVar.of(resource, name);
            return attachments != null ? term.withAttachments(attachments) : term;
        }

    }

}