package mb.statix.random.util;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.PartialFunction0;
import org.metaborg.util.functions.Predicate0;

import com.google.common.collect.Streams;

public class StreamUtil {

    public static <T> Stream<T> generate(PartialFunction0<T> generator) {
        final AtomicReference<Optional<T>> next = new AtomicReference<>();
        return generate(() -> {
            Optional<T> t;
            if((t = next.get()) == null) {
                next.set((t = generator.apply()));
            }
            return t.isPresent();
        }, () -> {
            Optional<T> t;
            if((t = next.get()) == null) {
                next.set((t = generator.apply()));
            }
            next.set(null);
            return t.get();
        });
    }

    public static <T> Stream<T> generate(Predicate0 hasNext, Function0<T> next) {
        return Streams.stream(new Iterator<T>() {

            @Override public boolean hasNext() {
                return hasNext.test();
            }

            @Override public T next() {
                return next.apply();
            }

        });
    }

}