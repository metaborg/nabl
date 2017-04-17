package org.metaborg.meta.nabl2.terms;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface INilTerm extends IListTerm {

    INilTerm withAttachments(ImmutableClassToInstanceMap<Object> value);

}