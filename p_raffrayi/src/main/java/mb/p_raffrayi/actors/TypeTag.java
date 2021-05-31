package mb.p_raffrayi.actors;

public class TypeTag<T> {

    private final Class<? super T> type;

    private TypeTag(Class<? super T> type) {
        this.type = type;
    }

    public Class<?> type() {
        return type;
    }

    public static <T> TypeTag<T> of(Class<? super T> type) {
        return new TypeTag<>(type);
    }

}