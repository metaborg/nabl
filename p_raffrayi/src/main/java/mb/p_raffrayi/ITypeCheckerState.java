package mb.p_raffrayi;

import java.util.Optional;

public interface ITypeCheckerState<S, L, D> {

    Optional<D> tryGetExternalDatum(D datum);

}
