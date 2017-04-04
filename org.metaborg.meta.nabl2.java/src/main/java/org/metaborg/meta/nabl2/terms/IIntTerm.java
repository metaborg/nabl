package org.metaborg.meta.nabl2.terms;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface IIntTerm extends ITerm {

    int getValue();

    IIntTerm withAttachments(ImmutableClassToInstanceMap<Object> value);

}