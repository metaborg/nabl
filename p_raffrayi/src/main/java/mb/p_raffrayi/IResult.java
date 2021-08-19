package mb.p_raffrayi;

import java.io.ObjectStreamException;
import java.io.Serializable;

public interface IResult<S, L, D> {

    D getExternalRepresentation(D datum);

    public class Empty<S, L, D> implements IResult<S, L, D>, Serializable {

        private static final long serialVersionUID = 42L;

        @SuppressWarnings("rawtypes") private static final Empty instance = new Empty();

        @SuppressWarnings("unchecked") public static <S, L, D> Empty<S, L, D> of() {
            return instance;
        }

        @Override public D getExternalRepresentation(D datum) {
            return datum;
        }

        private Object readResolve() throws ObjectStreamException {
            return instance;
        }

    }

}
