package mb.statix.scopegraph.path;

import mb.nabl2.util.collections.PSequence;

public interface IPath<V, L> {

    PSequence<V> scopes();

    PSequence<L> labels();

}