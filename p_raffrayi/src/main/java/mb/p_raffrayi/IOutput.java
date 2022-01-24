package mb.p_raffrayi;

public interface IOutput<S, L, D> {

    D getExternalRepresentation(D datum);

}
