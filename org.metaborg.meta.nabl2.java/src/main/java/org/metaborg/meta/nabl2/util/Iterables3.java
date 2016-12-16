package org.metaborg.meta.nabl2.util;

import java.util.Iterator;
import java.util.Optional;

import org.metaborg.meta.nabl2.util.functions.Function2;

import com.google.common.collect.Iterables;

public class Iterables3 {

    private static final class Zip2Iterable<R, T1, T2> implements Iterable<R> {

        private final Iterable<? extends T1> ts1;
        private final Iterable<? extends T2> ts2;
        private final Function2<T1,T2,R> f;

        public Zip2Iterable(Iterable<? extends T1> ts1, Iterable<? extends T2> ts2, Function2<T1,T2,R> f) {
            this.ts1 = ts1;
            this.ts2 = ts2;
            this.f = f;
        }

        @Override public Iterator<R> iterator() {
            return new Iterator<R>() {

                Iterator<? extends T1> it1 = ts1.iterator();
                Iterator<? extends T2> it2 = ts2.iterator();

                @Override public boolean hasNext() {
                    return it1.hasNext() && it2.hasNext();
                }

                @Override public R next() {
                    return f.apply(it1.next(), it2.next());
                }

            };
        }
    }

    public static <T1, T2, R> Iterable<R> zip(Iterable<? extends T1> ts1, Iterable<? extends T2> ts2,
            Function2<T1,T2,R> f) {
        return new Zip2Iterable<R,T1,T2>(ts1, ts2, f);
    }

    public static <T1, T2, R> Optional<Iterable<R>> zipStrict(Iterable<? extends T1> ts1, Iterable<? extends T2> ts2,
            Function2<T1,T2,R> f) {
        if (Iterables.size(ts1) != Iterables.size(ts2)) {
            return Optional.empty();
        } else {
            return Optional.of(new Zip2Iterable<R,T1,T2>(ts1, ts2, f));
        }
    }

}