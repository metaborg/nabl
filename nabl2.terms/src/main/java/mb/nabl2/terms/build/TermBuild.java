package mb.nabl2.terms.build;

import java.util.Arrays;
import java.util.LinkedList;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import mb.nabl2.terms.IApplTerm;
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

    public static class B {

        public final IApplTerm EMPTY_TUPLE = newAppl(Terms.TUPLE_OP, ImmutableList.of());

        public final IListTerm EMPTY_LIST = newNil();

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

        public IListTerm newList(ITerm... elems) {
            return newList(Arrays.asList(elems));

        }

        public IListTerm newList(Iterable<? extends ITerm> elems) {
            return newList(elems, null);
        }

        public IListTerm newList(Iterable<? extends ITerm> elems,
                @Nullable Iterable<ImmutableClassToInstanceMap<Object>> attachments) {
            LinkedList<ITerm> elemsQueue = Lists.newLinkedList(elems);
            LinkedList<ImmutableClassToInstanceMap<Object>> attachmentsQueue =
                    attachments != null ? Lists.newLinkedList(attachments) : null;
            if(attachmentsQueue != null && attachmentsQueue.size() != elemsQueue.size() + 1) {
                throw new IllegalArgumentException(
                        "Number of attachments does not correspond to number of elements in the list.");
            }
            IListTerm list = newNil();
            if(attachmentsQueue != null) {
                list = list.withAttachments(attachmentsQueue.removeLast());
            }
            return newListTail(elemsQueue, list, attachmentsQueue);
        }

        public IListTerm newListTail(Iterable<? extends ITerm> elems, IListTerm list) {
            return newListTail(elems, list, null);
        }

        public IListTerm newListTail(Iterable<? extends ITerm> elems, IListTerm list,
                @Nullable Iterable<ImmutableClassToInstanceMap<Object>> attachments) {
            LinkedList<ITerm> elemsQueue = Lists.newLinkedList(elems);
            LinkedList<ImmutableClassToInstanceMap<Object>> attachmentsQueue =
                    attachments != null ? Lists.newLinkedList(attachments) : null;
            if(attachmentsQueue != null && attachmentsQueue.size() != elemsQueue.size()) {
                throw new IllegalArgumentException(
                        "Number of attachments does not correspond to number of elements in the list.");
            }
            return newListTail(elemsQueue, list, attachmentsQueue);
        }

        private IListTerm newListTail(LinkedList<? extends ITerm> elems, IListTerm list,
                @Nullable LinkedList<ImmutableClassToInstanceMap<Object>> attachments) {
            while(!elems.isEmpty()) {
                list = newCons(elems.removeLast(), list);
                if(attachments != null) {
                    list = list.withAttachments(attachments.removeLast());
                }
            }
            return list;
        }

        public INilTerm newNil() {
            return newNil(null);
        }

        public INilTerm newNil(@Nullable ImmutableClassToInstanceMap<Object> attachments) {
            final INilTerm term = ImmutableNilTerm.of();
            return attachments != null ? term.withAttachments(attachments) : term;
        }

        public IConsTerm newCons(ITerm head, IListTerm tail) {
            return newCons(head, tail, null);
        }

        public IConsTerm newCons(ITerm head, IListTerm tail, @Nullable ImmutableClassToInstanceMap<Object> attachments) {
            final IConsTerm term = ImmutableConsTerm.of(head, tail);
            return attachments != null ? term.withAttachments(attachments) : term;
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