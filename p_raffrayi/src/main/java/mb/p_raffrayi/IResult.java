package mb.p_raffrayi;

public interface IResult<S, L, D> {

    D getExternalRepresentation(D datum);

    class Empty<S, L, D> implements IResult<S, L, D> {

        @SuppressWarnings("rawtypes") private static final Empty unit = new Empty();

        @SuppressWarnings("unchecked") public static <S, L, D> Empty<S, L, D> of() {
            return unit;
        }

        @Override public D getExternalRepresentation(D datum) {
            return datum;
        }

    }

}
