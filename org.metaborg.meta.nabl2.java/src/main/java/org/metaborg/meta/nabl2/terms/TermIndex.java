package org.metaborg.meta.nabl2.terms;

import org.immutables.value.Value;

@Value.Immutable
public interface TermIndex extends IAnnotation {

    @Value.Parameter String getResource();

    @Value.Parameter int getId();

}
