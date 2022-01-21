package mb.p_raffrayi;

public interface IResult<S, L, D> {

    D getExternalRepresentation(D datum);

}
