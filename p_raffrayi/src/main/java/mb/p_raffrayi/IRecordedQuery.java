package mb.p_raffrayi;

import java.util.Optional;
import java.util.Set;

import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;
import mb.scopegraph.patching.IPatchCollection;

public interface IRecordedQuery<S, L, D> {

    ScopePath<S, L> scopePath();

    LabelWf<L> labelWf();

    DataWf<S, L, D> dataWf();

    LabelOrder<L> labelOrder(); // TODO: remove?

    DataLeq<S, L, D> dataLeq(); // TODO: remove?

    Set<IRecordedQuery<S, L, D>> transitiveQueries();

    Set<IRecordedQuery<S, L, D>> predicateQueries();

    // TODO: remove result
    Optional<Env<S, L, D>> result();

    IRecordedQuery<S, L, D> patch(IPatchCollection.Immutable<S> patches);

}
