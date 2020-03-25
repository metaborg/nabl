package mb.nabl2.terms.build;

import java.util.Arrays;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IBlobTerm;
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;

public class TermBuild {

    public static final B B = new B();

    public static class B {

        public final IApplTerm EMPTY_TUPLE = newAppl(Terms.TUPLE_OP, ImmutableList.of());

        public IApplTerm newAppl(String op, ITerm... args) {
            return newAppl(op, Arrays.asList(args));
        }

        public IApplTerm newAppl(String op, Iterable<? extends ITerm> args) {
            return newAppl(op, args, null);
        }

        public IApplTerm newAppl(String op, Iterable<? extends ITerm> args,
                @Nullable ImmutableClassToInstanceMap<Object> attachments) {
            final IApplTerm term = ImmutableApplTerm.of(op, args);
            return attachments != null ? term.withAttachments(attachments) : term;
        }

        public ITerm newTuple(ITerm... args) {
            return newTuple(Arrays.asList(args));
        }

        public ITerm newTuple(Iterable<? extends ITerm> args) {
            return newTuple(args, null);
        }

        public ITerm newTuple(Iterable<? extends ITerm> args,
                @Nullable ImmutableClassToInstanceMap<Object> attachments) {
            return Iterables.size(args) == 1 ? Iterables.getOnlyElement(args)
                    : newAppl(Terms.TUPLE_OP, args, attachments);
        }

        public IStringTerm newString(String value) {
            return newString(value, null);
        }

        public IStringTerm newString(String value, @Nullable ImmutableClassToInstanceMap<Object> attachments) {
            final IStringTerm term = ImmutableStringTerm.of(value);
            return attachments != null ? term.withAttachments(attachments) : term;
        }

        public IIntTerm newInt(int value) {
            return newInt(value, null);
        }

        public IIntTerm newInt(int value, @Nullable ImmutableClassToInstanceMap<Object> attachments) {
            final IIntTerm term = ImmutableIntTerm.of(value);
            return attachments != null ? term.withAttachments(attachments) : term;
        }


        public IBlobTerm newBlob(Object value) {
            return newBlob(value, null);
        }

        public IBlobTerm newBlob(Object value, @Nullable ImmutableClassToInstanceMap<Object> attachments) {
            final IBlobTerm term = ImmutableBlobTerm.of(value);
            return attachments != null ? term.withAttachments(attachments) : term;
        }

        public ITermVar newVar(String resource, String name) {
            return newVar(resource, name, null);
        }

        public ITermVar newVar(String resource, String name, @Nullable ImmutableClassToInstanceMap<Object> attachments) {
            final ITermVar term = ImmutableTermVar.of(resource, name);
            return attachments != null ? term.withAttachments(attachments) : term;
        }

    }

}
