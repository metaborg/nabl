package mb.statix.concurrent.p_raffrayi.diff;

public class Removed<S, L, D> implements IScopeDiff<S, L, D> {

    @SuppressWarnings("rawtypes") private static final Removed REMOVED = new Removed();

    private Removed() {
    }

    @SuppressWarnings("unchecked") public static <S, L, D> Removed<S, L, D> of() {
        return REMOVED;
    }

}
