package mb.statix.util.function;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Serializable variant of {@link Supplier}.
 *
 * @param <T> the type of results supplied by this supplier
 */
@FunctionalInterface
public interface SerializableSupplier<T> extends Supplier<T>, Serializable {

}
