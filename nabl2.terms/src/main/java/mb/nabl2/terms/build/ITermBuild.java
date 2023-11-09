package mb.nabl2.terms.build;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import jakarta.annotation.Nullable;

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

public interface ITermBuild {

    default IApplTerm newAppl(String op, ITerm... args) {
        return newAppl(op, Arrays.asList(args));
    }

    default IApplTerm newAppl(String op, Iterable<? extends ITerm> args) {
        return newAppl(op, args, null);
    }

    IApplTerm newAppl(String op, Iterable<? extends ITerm> args,
            @Nullable IAttachments attachments);


    default ITerm newTuple(ITerm... args) {
        return newTuple(Arrays.asList(args));
    }

    default ITerm newTuple(Collection<? extends ITerm> args) {
        return newTuple(args, null);
    }

    default ITerm newTuple(Collection<? extends ITerm> args, @Nullable IAttachments attachments) {
        return args.size() == 1 ? args.iterator().next() : newAppl(Terms.TUPLE_OP, args, attachments);
    }


    default IListTerm newList(ITerm... elems) {
        return newList(Arrays.asList(elems));
    }

    default IListTerm newList(Collection<? extends ITerm> elems) {
        return newList(elems, null);
    }

    default IListTerm newList(Collection<? extends ITerm> elems,
            @Nullable Collection<IAttachments> attachments) {
        LinkedList<ITerm> elemsQueue = new LinkedList(elems);
        LinkedList<IAttachments> attachmentsQueue =
                attachments != null ? new LinkedList(attachments) : null;
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

    default IListTerm newListTail(Collection<? extends ITerm> elems, IListTerm list) {
        return newListTail(elems, list, null);
    }

    default IListTerm newListTail(Collection<? extends ITerm> elems, IListTerm list,
            @Nullable Collection<IAttachments> attachments) {
        LinkedList<ITerm> elemsQueue = new LinkedList(elems);
        LinkedList<IAttachments> attachmentsQueue =
                attachments != null ? new LinkedList(attachments) : null;
        if(attachmentsQueue != null && attachmentsQueue.size() != elemsQueue.size()) {
            throw new IllegalArgumentException(
                    "Number of attachments does not correspond to number of elements in the list.");
        }
        return newListTail(elemsQueue, list, attachmentsQueue);
    }

    default IListTerm newListTail(LinkedList<? extends ITerm> elems, IListTerm list,
            @Nullable LinkedList<IAttachments> attachments) {
        while(!elems.isEmpty()) {
            list = newCons(elems.removeLast(), list);
            if(attachments != null) {
                list = list.withAttachments(attachments.removeLast());
            }
        }
        return list;
    }


    default INilTerm newNil() {
        return newNil(null);
    }

    public INilTerm newNil(@Nullable IAttachments attachments);


    default IConsTerm newCons(ITerm head, IListTerm tail) {
        return newCons(head, tail, null);
    }

    IConsTerm newCons(ITerm head, IListTerm tail, @Nullable IAttachments attachments);


    default IStringTerm newString(String value) {
        return newString(value, null);
    }

    IStringTerm newString(String value, @Nullable IAttachments attachments);


    default IIntTerm newInt(int value) {
        return newInt(value, null);
    }

    IIntTerm newInt(int value, @Nullable IAttachments attachments);


    default IBlobTerm newBlob(Object value) {
        return newBlob(value, null);
    }

    IBlobTerm newBlob(Object value, @Nullable IAttachments attachments);


    default ITermVar newVar(String resource, String name) {
        return newVar(resource, name, null);
    }

    ITermVar newVar(String resource, String name, @Nullable IAttachments attachments);

}