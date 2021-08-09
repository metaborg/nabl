package mb.p_raffrayi.impl.diff;

import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

public class Removed<S, L, D> implements IScopeDiff<S, L, D> {

    @SuppressWarnings("rawtypes") private static final Removed REMOVED = new Removed();

    private Removed() {
    }

    @SuppressWarnings("unchecked") public static <S, L, D> Removed<S, L, D> of() {
        return REMOVED;
    }

    @Override public <T> T match(Function1<Matched<S, L, D>, T> onMatched, Function0<T> onRemoved) {
        return onRemoved.apply();
    }

}
