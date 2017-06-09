package org.metaborg.meta.nabl2.terms;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface IStringTerm extends ITerm {

    String getValue();

    IStringTerm withAttachments(ImmutableClassToInstanceMap<Object> value);

}