package org.metaborg.meta.nabl2.terms;

import java.util.List;

public interface IApplTerm extends ITerm {

    String getOp();

    int getArity();

    List<ITerm> getArgs();

}