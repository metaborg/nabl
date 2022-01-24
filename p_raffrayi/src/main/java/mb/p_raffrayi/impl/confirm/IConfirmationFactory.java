package mb.p_raffrayi.impl.confirm;

public interface IConfirmationFactory<S, L, D> {

    IConfirmation<S, L, D> getConfirmation(IConfirmationContext<S, L, D> context);

}
