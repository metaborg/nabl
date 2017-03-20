package org.metaborg.meta.nabl2.scopegraph.fixedpoint.generalized;

public interface Namespace<R, D> {

    boolean match(R ref, D decl);

}