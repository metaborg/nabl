package org.metaborg.meta.nabl2.terms;

import java.util.List;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface IApplTerm extends ITerm {

    String getOp();

    int getArity();

    List<ITerm> getArgs();

    @Override IApplTerm withAttachments(ImmutableClassToInstanceMap<Object> value);

    @Override IApplTerm withLocked(boolean locked);

}