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

import com.google.common.collect.ImmutableClassToInstanceMap;

public class GenericTerms {

    public static IApplTerm newAppl(String op, Iterable<ITerm> args) {
        return ImmutableApplTerm.of(op, args);
    }

    public static IApplTerm newAppl(String op, Iterable<ITerm> args, ImmutableClassToInstanceMap<Object> attachments) {
        return ImmutableApplTerm.of(op, args).setAttachments(attachments);
    }


    public static IListTerm newList(Iterable<ITerm> elems) {
        return newListTail(elems, newNil());
    }

    public static IListTerm newListTail(Iterable<ITerm> elems, IListTerm list) {
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
        return ImmutableConsTerm.of(head, tail).setAttachments(attachments);
    }


    public static INilTerm newNil() {
        return ImmutableNilTerm.of();
    }

    public static INilTerm newNil(ImmutableClassToInstanceMap<Object> attachments) {
        return ImmutableNilTerm.of().setAttachments(attachments);
    }


    public static IStringTerm newString(String value) {
        return ImmutableStringTerm.of(value);
    }

    public static IStringTerm newString(String value, ImmutableClassToInstanceMap<Object> attachments) {
        return ImmutableStringTerm.of(value).setAttachments(attachments);
    }


    public static IIntTerm newInt(int value) {
        return ImmutableIntTerm.of(value);
    }

    public static IIntTerm newInt(int value, ImmutableClassToInstanceMap<Object> attachments) {
        return ImmutableIntTerm.of(value).setAttachments(attachments);
    }


    public static ITermVar newVar(String resource, String name) {
        return ImmutableTermVar.of(resource, name);
    }

}