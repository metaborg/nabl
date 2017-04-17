package org.metaborg.meta.nabl2.terms;

import java.util.List;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface IApplTerm extends ITerm {

    String getOp();

    int getArity();

    List<ITerm> getArgs();

    IApplTerm withAttachments(ImmutableClassToInstanceMap<Object> value);

}