package org.metaborg.meta.nabl2.scopegraph;

public interface INameResolution<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Iterable<O> resolve(O ref);

}