package mb.statix.p_raffrayi;

import java.util.Map;

public interface IResult<S, L, D, R> {

    Map<String, IUnitResult<S, L, D, R>> unitResults();

}