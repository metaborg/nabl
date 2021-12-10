package mb.p_raffrayi.impl.confirm;

import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.IRecordedQuery;

public class EagerConfirmation<S, L, D> extends OptimisticConfirmation<S, L, D> {

    public EagerConfirmation(IConfirmationContext<S, L, D> context) {
        super(context);
    }

    @Override public IFuture<ConfirmResult<S>> confirm(IRecordedQuery<S, L, D> query) {
        return confirmSingle(query);
    }

    public static <S, L, D> IConfirmationFactory<S, L, D> factory() {
        return EagerConfirmation::new;
    }

}
