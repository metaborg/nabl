package org.metaborg.meta.nabl2.scopegraph;

public interface IActiveScopes<S, L> {

    public boolean isComplete();

    public boolean isOpen(S scope, L label);

}