package mb.p_raffrayi.impl.confirm;

import java.util.Optional;

import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.impl.IQueryAnswer;
import mb.p_raffrayi.impl.envdiff.IEnvDiff;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public interface IConfirmationContext<S, L, D> {

    IFuture<IQueryAnswer<S, L, D>> query(ScopePath<S, L> scopePath, LabelWf<L> labelWf, LabelOrder<L> labelOrder,
            DataWf<S, L, D> dataWf, DataLeq<S, L, D> dataEquiv);

    IFuture<IQueryAnswer<S, L, D>> queryPrevious(ScopePath<S, L> scopePath, LabelWf<L> labelWf, DataWf<S, L, D> dataWf,
            LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv);

    IFuture<Optional<ConfirmResult<S, L, D>>> externalConfirm(ScopePath<S, L> path, LabelWf<L> labelWf,
            DataWf<S, L, D> dataWf, boolean prevEnvEmpty);

    IFuture<IEnvDiff<S, L, D>> envDiff(ScopePath<S, L> path, LabelWf<L> labelWf);

    IFuture<Optional<S>> match(S scope);
}
