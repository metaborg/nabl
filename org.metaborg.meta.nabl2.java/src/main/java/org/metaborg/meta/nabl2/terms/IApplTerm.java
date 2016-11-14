package org.metaborg.meta.nabl2.terms;

public interface IApplTerm extends ITerm {

    String getOp();

    int getArity();

    Iterable<ITerm> getArgs();

}