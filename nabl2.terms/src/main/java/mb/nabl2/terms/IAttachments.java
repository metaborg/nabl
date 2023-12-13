package mb.nabl2.terms;

import jakarta.annotation.Nullable;

public interface IAttachments {

    boolean isEmpty();

    /**
     * Gets the attachment of the specified class.
     *
     * @param cls
     *            the class of the attachment
     * @param <T>
     *            the type of attachment
     * @return the attachment, if found; otherwise, {@code null}
     */
    @Nullable <T> T get(Class<T> cls);

    Builder toBuilder();

    interface Builder {

        <T> void put(Class<T> cls, T value);

        IAttachments build();

    }

}
