package mb.nabl2.terms;

import javax.annotation.Nullable;

public interface IAttachments {

    boolean isEmpty();

    <T> @Nullable T get(Class<T> cls);

    Builder toBuilder();

    interface Builder {

        <T> void put(Class<T> cls, T value);

        IAttachments build();

        boolean isEmpty();

    }

}