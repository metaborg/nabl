package mb.statix.concurrent.p_raffrayi;

import java.util.Map;

public interface IBrokerResult<S, L, D, R> {

    Map<String, IUnitResult<S, L, D, R>> unitResults();

}