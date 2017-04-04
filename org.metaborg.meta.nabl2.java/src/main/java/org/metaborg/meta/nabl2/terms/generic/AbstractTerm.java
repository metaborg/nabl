package org.metaborg.meta.nabl2.terms.generic;

import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;

import com.google.common.collect.ImmutableClassToInstanceMap;

public abstract class AbstractTerm implements ITerm {

    @Value.Auxiliary @Value.Default public ImmutableClassToInstanceMap<Object> getAttachments() {
        return ImmutableClassToInstanceMap.<Object>builder().build();
    }

}