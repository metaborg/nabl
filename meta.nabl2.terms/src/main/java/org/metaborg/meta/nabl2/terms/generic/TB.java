package org.metaborg.meta.nabl2.terms.generic;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.IConsTerm;
import org.metaborg.meta.nabl2.terms.IIntTerm;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.INilTerm;
import org.metaborg.meta.nabl2.terms.IStringTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.Terms;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.Lists;

public class TB {

    public static final IApplTerm EMPTY_TUPLE = newTuple();

    public static final IListTerm EMPTY_LIST = newNil();

    public static IApplTerm newAppl(String op, ITerm... args) {
        return ImmutableApplTerm.of(op, Arrays.asList(args));
    }

    public static IApplTerm newAppl(String op, Collection<? extends ITerm> args) {
        return ImmutableApplTerm.of(op, args);
    }

    public static IApplTerm newAppl(String op, Collection<? extends ITerm> args,
            ImmutableClassToInstanceMap<Object> attachments) {
        return ImmutableApplTerm.of(op, args).withAttachments(attachments);
    }

    public static IApplTerm newTuple(ITerm... args) {
        return newAppl(Terms.TUPLE_OP, args);
    }

    public static IApplTerm newTuple(Collection<? extends ITerm> args) {
        return newAppl(Terms.TUPLE_OP, args);
    }

    public static IApplTerm newTuple(Collection<? extends ITerm> args, ImmutableClassToInstanceMap<Object> attachments) {
        return newAppl(Terms.TUPLE_OP, args, attachments);
    }


    public static IListTerm newList(ITerm... elems) {
        return newList(Arrays.asList(elems));
    }

    public static IListTerm newList(Collection<? extends ITerm> elems) {
        return newListTail(elems, newNil());
    }

    public static IListTerm newList(Collection<? extends ITerm> elems,
            List<ImmutableClassToInstanceMap<Object>> attachments) {
        if(attachments != null && attachments.size() != elems.size() + 1) {
            throw new IllegalArgumentException(
                    "Number of attachments does not correspond to number of elements in the list.");
        }
        LinkedList<ITerm> elemsQueue = Lists.newLinkedList(elems);
        LinkedList<ImmutableClassToInstanceMap<Object>> attachmentsQueue =
                attachments != null ? Lists.newLinkedList(attachments) : null;
        IListTerm list = newNil();
        if(attachmentsQueue != null) {
            list = list.withAttachments(attachmentsQueue.removeLast());
        }
        return newListTail(elemsQueue, list, attachmentsQueue);
    }

    public static IListTerm newListTail(Collection<? extends ITerm> elems, IListTerm list) {
        return newListTail(elems, list, null);
    }

    public static IListTerm newListTail(Collection<? extends ITerm> elems, IListTerm list,
            @Nullable List<ImmutableClassToInstanceMap<Object>> attachments) {
        if(attachments != null && attachments.size() != elems.size()) {
            throw new IllegalArgumentException(
                    "Number of attachments does not correspond to number of elements in the list.");
        }
        LinkedList<ITerm> elemsQueue = Lists.newLinkedList(elems);
        LinkedList<ImmutableClassToInstanceMap<Object>> attachmentsQueue =
                attachments != null ? Lists.newLinkedList(attachments) : null;
        return newListTail(elemsQueue, list, attachmentsQueue);
    }

    private static IListTerm newListTail(LinkedList<? extends ITerm> elems, IListTerm list,
            @Nullable LinkedList<ImmutableClassToInstanceMap<Object>> attachments) {
        while(!elems.isEmpty()) {
            list = TB.newCons(elems.removeLast(), list);
            if(attachments != null) {
                list = list.withAttachments(attachments.removeLast());
            }
        }
        return list;
    }

    public static IConsTerm newCons(ITerm head, IListTerm tail) {
        return ImmutableConsTerm.of(head, tail);
    }

    public static IConsTerm newCons(ITerm head, IListTerm tail, ImmutableClassToInstanceMap<Object> attachments) {
        return ImmutableConsTerm.of(head, tail).withAttachments(attachments);
    }


    public static INilTerm newNil() {
        return ImmutableNilTerm.builder().build();
    }

    public static INilTerm newNil(ImmutableClassToInstanceMap<Object> attachments) {
        return ImmutableNilTerm.builder().attachments(attachments).build();
    }


    public static IStringTerm newString(String value) {
        return ImmutableStringTerm.of(value);
    }

    public static IStringTerm newString(String value, ImmutableClassToInstanceMap<Object> attachments) {
        return ImmutableStringTerm.of(value).withAttachments(attachments);
    }


    public static IIntTerm newInt(int value) {
        return ImmutableIntTerm.of(value);
    }

    public static IIntTerm newInt(int value, ImmutableClassToInstanceMap<Object> attachments) {
        return ImmutableIntTerm.of(value).withAttachments(attachments);
    }


    public static ITermVar newVar(String resource, String name) {
        return ImmutableTermVar.of(resource, name);
    }

}