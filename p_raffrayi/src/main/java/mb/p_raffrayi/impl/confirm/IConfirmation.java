package mb.p_raffrayi.impl.confirm;

import java.util.Optional;

import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public interface IConfirmation<S, L, D> {

    IFuture<Optional<BiMap.Immutable<S>>> confirm(java.util.Set<IRecordedQuery<S, L, D>> queries);

    IFuture<Optional<BiMap.Immutable<S>>> confirm(ScopePath<S, L> path, LabelWf<L> labelWF, DataWf<S, L, D> dataWF,
            boolean prevEnvEmpty);

}