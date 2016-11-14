package org.metaborg.meta.nabl2.terms;

public interface ITupleTerm extends ITerm {

    int getArity();

    Iterable<ITerm> getArgs();

}