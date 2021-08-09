package mb.p_raffrayi.impl.diff;

import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

public interface IScopeDiff<S, L, D> {

    <T> T match(Function1<Matched<S, L, D>, T> onMatched, Function0<T> onRemoved);

}
