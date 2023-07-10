package mb.p_raffrayi.impl;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.IRecordedQuery;
import mb.scopegraph.oopsla20.reference.Env;

public interface IQueryAnswer<S, L, D> {

    Env<S, L, D> env();

    // TODO: replace with ExtQueries.
    Set.Immutable<IRecordedQuery<S, L, D>> transitiveQueries();

    Set.Immutable<IRecordedQuery<S, L, D>> predicateQueries();

}
