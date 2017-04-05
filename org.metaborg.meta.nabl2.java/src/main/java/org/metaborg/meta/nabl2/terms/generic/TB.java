package org.metaborg.meta.nabl2.terms.generic;

import java.util.LinkedList;

import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.IConsTerm;
import org.metaborg.meta.nabl2.terms.IIntTerm;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.INilTerm;
import org.metaborg.meta.nabl2.terms.IStringTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.Terms;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableClassToInstanceMap;

public class TB {

    public static final IApplTerm EMPTY_TUPLE = newTuple();

    public static final IListTerm EMPTY_LIST = newNil();
 
    public static IApplTerm newAppl(String op, ITerm... args) {
        return ImmutableApplTerm.of(op, Iterables2.from(args));
    }

    public static IApplTerm newAppl(String op, Iterable<? extends ITerm> args) {
        return ImmutableApplTerm.of(op, args);
    }

    public static IApplTerm newAppl(String op, Iterable<? extends ITerm> args,
            ImmutableClassToInstanceMap<Object> attachments) {
        return ImmutableApplTerm.of(op, args).withAttachments(attachments);
    }


    public static IApplTerm newTuple(ITerm... args) {
        return newAppl(Terms.TUPLE_OP, args);
    }

    public static IApplTerm newTuple(Iterable<? extends ITerm> args) {
        return newAppl(Terms.TUPLE_OP, args);
    }

    public static IApplTerm newTuple(Iterable<? extends ITerm> args, ImmutableClassToInstanceMap<Object> attachments) {
        return newAppl(Terms.TUPLE_OP, args, attachments);
    }


    public static IListTerm newList(ITerm... elems) {
        return newList(Iterables2.from(elems));
    }

    public static IListTerm newList(Iterable<? extends ITerm> elems) {
        return newListTail(elems, newNil());
    }

    public static IListTerm newListTail(Iterable<? extends ITerm> elems, IListTerm list) {
        LinkedList<ITerm> reverse = new LinkedList<>();
        for (ITerm elem : elems) {
            reverse.addFirst(elem);
        }
        for (ITerm elem : reverse) {
            list = newCons(elem, list);
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