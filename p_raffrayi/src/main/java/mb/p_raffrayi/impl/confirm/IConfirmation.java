package mb.p_raffrayi.impl.confirm;

import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;

public interface IConfirmation<S, L, D> {

    IFuture<ConfirmResult<S, L, D>> confirm(IRecordedQuery<S, L, D> query);

    IFuture<ConfirmResult<S, L, D>> confirm(S scope, LabelWf<L> labelWF, DataWf<S, L, D> dataWF, boolean prevEnvEmpty);

}
