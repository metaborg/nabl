package org.metaborg.meta.nabl2.terms;

public interface ITermFactory {

    IApplTerm newAppl(String op, Iterable<ITerm> args);


    IListTerm newList(Iterable<ITerm> elems);

    IConsTerm newCons(ITerm head, IListTerm tail);

    INilTerm newNil();


    IStringTerm newString(String value);

    IIntTerm newInt(int value);

    ITermVar newVar(String resource, String name);

}