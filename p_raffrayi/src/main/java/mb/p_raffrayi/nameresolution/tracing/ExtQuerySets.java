package mb.p_raffrayi.nameresolution.tracing;

public final class ExtQuerySets {

    // Not in `AExtQuerySet` to prevent deadlock in class loading.
    @SuppressWarnings("rawtypes") private static final ExtQuerySet EMPTY = ExtQuerySet.builder().build();

    private ExtQuerySets() {
    }

    @SuppressWarnings("unchecked") public static <S, L, D> ExtQuerySet<S, L, D> empty() {
        return EMPTY;
    }

}
