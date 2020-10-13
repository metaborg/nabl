package mb.nabl2.terms;

public interface IAttachments {

    boolean isEmpty();

    <T> T get(Class<T> cls);

    Builder toBuilder();

    interface Builder {

        <T> void put(Class<T> cls, T value);

        IAttachments build();

        boolean isEmpty();

    }

}
