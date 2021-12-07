package mb.p_raffrayi;

import java.util.Set;

import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;
import mb.scopegraph.patching.IPatchCollection;

public interface IRecordedQuery<S, L, D> {

    ScopePath<S, L> scopePath();

    LabelWf<L> labelWf();

    DataWf<S, L, D> dataWf();

    Set<IRecordedQuery<S, L, D>> transitiveQueries();

    Set<IRecordedQuery<S, L, D>> predicateQueries();

    boolean empty();

    IRecordedQuery<S, L, D> patch(IPatchCollection.Immutable<S> patches);

}
